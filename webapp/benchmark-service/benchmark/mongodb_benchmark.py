import time
import threading
import statistics
from typing import Dict, List, Optional
from concurrent.futures import ThreadPoolExecutor
from pymongo import MongoClient
from pymongo.collection import Collection
from pymongo.database import Database
import json
import os
from datetime import datetime
import psutil
from threading import Lock

class MongoDBBenchmark:
    def __init__(self):
        # Configuration constants
        self.HOST = "127.0.0.1"
        self.DATABASE_NAME = "ycsb"
        self.COLLECTION_NAME = "usertable"
        self.RECORD_COUNT = 1000000
        self.OPERATION_COUNT = 1000000
        self.THREAD_COUNTS = [1, 2, 4]  # List of thread counts to test

        # Define workload configurations
        self.WORKLOADS = [
            {
                "name": "Balanced",
                "read_proportion": 0.5,
                "update_proportion": 0.5,
            },
            {
                "name": "Read-Heavy",
                "read_proportion": 0.95,
                "update_proportion": 0.05,
            },
            {
                "name": "Read-Only",
                "read_proportion": 1.0,
                "update_proportion": 0.0,
            },
            {
                "name": "Update-Heavy",
                "read_proportion": 0.05,
                "update_proportion": 0.95,
            }
        ]

        # Metrics
        self.read_counter = 0
        self.update_counter = 0
        self.insert_counter = 0
        self.scan_counter = 0
        self.error_counter = 0

        # Thread-safe counters and locks
        self.loaded_records = 0
        self.loaded_records_lock = Lock()
        self.last_progress_time = time.time()
        self.last_progress_time_lock = Lock()

        # Latency lists
        self.read_latencies: List[float] = []
        self.update_latencies: List[float] = []
        self.insert_latencies: List[float] = []
        self.scan_latencies: List[float] = []

        # Phase timing
        self.load_phase_start_time = 0
        self.load_phase_end_time = 0
        self.run_phase_start_time = 0
        self.run_phase_end_time = 0

        # MongoDB client
        self.client: Optional[MongoClient] = None
        self.db: Optional[Database] = None
        self.collection: Optional[Collection] = None

        # Resource monitoring
        self.cpu_usage = []
        self.ram_usage = []
        self.monitoring_thread = None
        self.should_monitor = False

        # Create metrics directory if it doesn't exist
        os.makedirs("metrics", exist_ok=True)

    def init_connection(self):
        """Initialize MongoDB connection"""
        self.client = MongoClient(f"mongodb://{self.HOST}:27017?retryWrites=false&maxPoolSize=100")
        self.db = self.client[self.DATABASE_NAME]
        self.collection = self.db[self.COLLECTION_NAME]

    def cleanup(self):
        """Cleanup MongoDB connection"""
        if self.client:
            if self.collection is not None:
                self.collection.delete_many({})
            self.client.close()

    def update_progress(self, records_processed: int):
        """Update and print progress in a thread-safe manner"""
        with self.loaded_records_lock:
            self.loaded_records += records_processed
            current_time = time.time()
            
            # Only print progress every 100,000 records or if it's been more than 5 seconds
            if (self.loaded_records % 100000 == 0 or 
                current_time - self.last_progress_time >= 5):
                
                with self.last_progress_time_lock:
                    self.last_progress_time = current_time
                
                elapsed_time = current_time - self.load_phase_start_time
                progress = (self.loaded_records / self.RECORD_COUNT) * 100
                rate = self.loaded_records / elapsed_time if elapsed_time > 0 else 0
                
                print(f"\rProgress: {progress:.1f}% ({self.loaded_records:,}/{self.RECORD_COUNT:,} records) "
                      f"Rate: {rate:.0f} records/sec "
                      f"Elapsed: {elapsed_time:.1f}s "
                      f"ETA: {(self.RECORD_COUNT - self.loaded_records) / rate:.1f}s if rate remains constant", 
                      end="", flush=True)

    def print_phase_metrics(self, phase_name: str, start_time: float, end_time: float, operation_count: int, latencies: List[float]):
        """Print metrics for a specific phase"""
        duration = end_time - start_time
        throughput = operation_count / duration if duration > 0 else 0

        print(f"\n=== {phase_name} Phase Results ===")
        print(f"Duration: {duration:.2f} seconds")
        print(f"Throughput: {throughput:.2f} ops/sec")
        
        if latencies:
            latencies_ms = [lat * 1000 for lat in latencies]  # Convert to milliseconds
            print(f"\nLatency Statistics (ms):")
            print(f"Count: {len(latencies)}")
            print(f"Min: {min(latencies_ms):.2f}")
            print(f"Mean: {statistics.mean(latencies_ms):.2f}")
            print(f"P50: {statistics.median(latencies_ms):.2f}")
            print(f"P75: {sorted(latencies_ms)[int(len(latencies_ms) * 0.75)]:.2f}")
            print(f"P90: {sorted(latencies_ms)[int(len(latencies_ms) * 0.90)]:.2f}")
            print(f"P99: {sorted(latencies_ms)[int(len(latencies_ms) * 0.99)]:.2f}")
            print(f"Max: {max(latencies_ms):.2f}")
        else:
            print("\nNo operations performed in this phase")

    def monitor_resources(self):
        """Monitor CPU and RAM usage in a separate thread"""
        process = psutil.Process()
        while self.should_monitor:
            self.cpu_usage.append(process.cpu_percent())
            self.ram_usage.append(process.memory_info().rss / 1024 / 1024)  # Convert to MB
            time.sleep(0.1)  # Sample every 100ms

    def start_monitoring(self):
        """Start resource monitoring"""
        self.cpu_usage = []
        self.ram_usage = []
        self.should_monitor = True
        self.monitoring_thread = threading.Thread(target=self.monitor_resources)
        self.monitoring_thread.start()

    def stop_monitoring(self):
        """Stop resource monitoring"""
        self.should_monitor = False
        if self.monitoring_thread:
            self.monitoring_thread.join()

    def print_resource_metrics(self, phase_name: str):
        """Print resource usage metrics for a phase"""
        if not self.cpu_usage or not self.ram_usage:
            return

        print(f"\n=== {phase_name} Resource Usage ===")
        print(f"CPU Usage:")
        print(f"  Average: {statistics.mean(self.cpu_usage):.2f}%")
        print(f"  Max: {max(self.cpu_usage):.2f}%")
        print(f"  Min: {min(self.cpu_usage):.2f}%")
        
        print(f"\nRAM Usage:")
        print(f"  Average: {statistics.mean(self.ram_usage):.2f} MB")
        print(f"  Max: {max(self.ram_usage):.2f} MB")
        print(f"  Min: {min(self.ram_usage):.2f} MB")

    def load_data(self, thread_count: int = None, record_count: int = None):
        """Load initial data into MongoDB"""
        print("Starting load phase...")
        self.load_phase_start_time = time.time()
        self.loaded_records = 0
        self.last_progress_time = self.load_phase_start_time
        self.insert_latencies = []  # Reset insert latencies

        # Use provided parameters or fall back to defaults
        thread_count = thread_count or self.THREAD_COUNTS[2]
        record_count = record_count or self.RECORD_COUNT

        def load_worker(start_record: int, end_record: int):
            local_processed = 0
            for i in range(start_record, end_record):
                key = f"user{i}"
                values = {
                    "field0": f"value{i}_0",
                    "field1": f"value{i}_1",
                    "field2": f"value{i}_2",
                    "field3": f"value{i}_3",
                    "field4": f"value{i}_4",
                    "field5": f"value{i}_5",
                    "field6": f"value{i}_6",
                    "field7": f"value{i}_7",
                    "field8": f"value{i}_8",
                    "field9": f"value{i}_9"
                }

                start_time = time.time()
                try:
                    self.collection.insert_one({"_id": key, **values})
                    self.insert_latencies.append(time.time() - start_time)
                    local_processed += 1
                    
                    # Update progress every 1000 records
                    if local_processed % 1000 == 0:
                        self.update_progress(1000)
                        local_processed = 0
                        
                except Exception as e:
                    print(f"\nError inserting record {key}: {e}")
                    self.error_counter += 1
            
            # Update any remaining records
            if local_processed > 0:
                self.update_progress(local_processed)

        records_per_thread = record_count // thread_count
        threads = []

        for i in range(thread_count):
            start_record = i * records_per_thread
            end_record = record_count if i == thread_count - 1 else (i + 1) * records_per_thread
            thread = threading.Thread(target=load_worker, args=(start_record, end_record))
            threads.append(thread)
            thread.start()

        for thread in threads:
            thread.join()

        self.load_phase_end_time = time.time()
        print("\nLoad phase completed.")
        
        # Print load phase metrics
        self.print_phase_metrics(
            "Load",
            self.load_phase_start_time,
            self.load_phase_end_time,
            record_count,
            self.insert_latencies
        )
        self.print_resource_metrics("Load")

    def run_benchmark(self, operation_count: int = None, thread_count: int = None, workload_config: dict = None):
        """Run the benchmark operations with specified parameters"""
        print("\nStarting benchmark run...")
        
        # Use provided parameters or fall back to defaults
        operation_count = operation_count or self.OPERATION_COUNT
        thread_count = thread_count or self.THREAD_COUNTS[0]
        workload_config = workload_config or self.WORKLOADS[0]  # Default to first workload if none provided
        
        print(f"\n=== Testing with {thread_count} threads ===")
        print(f"\n=== Starting {workload_config['name']} Workload ===")
        print(f"Read proportion: {workload_config['read_proportion']}")
        print(f"Update proportion: {workload_config['update_proportion']}")
        
        # Reset counters and latencies
        self.read_counter = 0
        self.update_counter = 0
        self.insert_counter = 0
        self.scan_counter = 0
        self.error_counter = 0
        self.read_latencies = []
        self.update_latencies = []
        
        self.run_phase_start_time = time.time()
        self.start_monitoring()

        def run_worker():
            for _ in range(operation_count // thread_count):
                key = f"user{hash(str(time.time())) % self.RECORD_COUNT}"
                random_value = time.time() % 1.0

                if random_value < workload_config['read_proportion']:
                    # Read operation
                    start_time = time.time()
                    try:
                        result = self.collection.find_one({"_id": key})
                        self.read_latencies.append(time.time() - start_time)
                        self.read_counter += 1
                    except Exception as e:
                        print(f"Error reading record {key}: {e}")
                        self.error_counter += 1

                elif random_value < workload_config['read_proportion'] + workload_config['update_proportion']:
                    # Update operation
                    values = {
                        "field0": f"updated_value_{time.time()}",
                        "field1": f"updated_value_{time.time()}",
                        "field2": f"updated_value_{time.time()}",
                        "field3": f"updated_value_{time.time()}",
                        "field4": f"updated_value_{time.time()}"
                    }
                    start_time = time.time()
                    try:
                        self.collection.update_one({"_id": key}, {"$set": values})
                        self.update_latencies.append(time.time() - start_time)
                        self.update_counter += 1
                    except Exception as e:
                        print(f"Error updating record {key}: {e}")
                        self.error_counter += 1

        threads = []
        for _ in range(thread_count):
            thread = threading.Thread(target=run_worker)
            threads.append(thread)
            thread.start()

        for thread in threads:
            thread.join()

        self.run_phase_end_time = time.time()
        self.stop_monitoring()
        print(f"\n{workload_config['name']} workload completed.")

        # Print run phase metrics for this workload
        print(f"\n=== {workload_config['name']} Workload Results ===")
        duration = self.run_phase_end_time - self.run_phase_start_time
        total_operations = self.read_counter + self.update_counter
        throughput = total_operations / duration if duration > 0 else 0
        
        print(f"Duration: {duration:.2f} seconds")
        print(f"Total Operations: {total_operations}")
        print(f"Throughput: {throughput:.2f} ops/sec")
        
        if self.read_latencies:
            print("\nRead Operations:")
            self.print_phase_metrics("Read", 0, 0, self.read_counter, self.read_latencies)
        else:
            print("\nNo read operations performed")
        
        if self.update_latencies:
            print("\nUpdate Operations:")
            self.print_phase_metrics("Update", 0, 0, self.update_counter, self.update_latencies)
        else:
            print("\nNo update operations performed")

        self.print_resource_metrics(f"{workload_config['name']} Run")

        # Save metrics for this workload and thread count
        self.save_metrics(workload_config['name'], thread_count)

    def calculate_percentiles(self, latencies: List[float]) -> Dict[str, float]:
        """Calculate percentile statistics for latencies"""
        if not latencies:
            return {}

        latencies.sort()
        return {
            "min": latencies[0],
            "max": latencies[-1],
            "mean": statistics.mean(latencies),
            "p50": latencies[int(len(latencies) * 0.50)],
            "p75": latencies[int(len(latencies) * 0.75)],
            "p90": latencies[int(len(latencies) * 0.90)],
            "p99": latencies[int(len(latencies) * 0.99)]
        }

    def save_metrics(self, workload_name: str = "", thread_count: int = 1):
        """Save benchmark metrics to file and print results"""
        metrics = {
            "workload_name": workload_name,
            "thread_count": thread_count,
            "record_count": self.RECORD_COUNT,
            "operation_count": self.OPERATION_COUNT,
            "load_phase_time": self.load_phase_end_time - self.load_phase_start_time,
            "run_phase_time": self.run_phase_end_time - self.run_phase_start_time,
            "read_latencies": self.calculate_percentiles(self.read_latencies),
            "update_latencies": self.calculate_percentiles(self.update_latencies),
            "insert_latencies": self.calculate_percentiles(self.insert_latencies),
            "scan_latencies": self.calculate_percentiles(self.scan_latencies),
            "operation_counts": {
                "read": self.read_counter,
                "update": self.update_counter,
                "insert": self.insert_counter,
                "scan": self.scan_counter,
                "errors": self.error_counter
            },
            "resource_usage": {
                "cpu": {
                    "average": statistics.mean(self.cpu_usage) if self.cpu_usage else 0,
                    "max": max(self.cpu_usage) if self.cpu_usage else 0,
                    "min": min(self.cpu_usage) if self.cpu_usage else 0
                },
                "ram": {
                    "average": statistics.mean(self.ram_usage) if self.ram_usage else 0,
                    "max": max(self.ram_usage) if self.ram_usage else 0,
                    "min": min(self.ram_usage) if self.ram_usage else 0
                }
            }
        }

        # Save to file in metrics directory
        filename = f"metrics/{self.RECORD_COUNT}_mongodb_{thread_count}_{workload_name}.json"
        with open(filename, "w") as f:
            json.dump(metrics, f, indent=2)

        print(f"\nDetailed metrics have been saved to {filename}")

def main():
    benchmark = MongoDBBenchmark()
    try:
        benchmark.init_connection()
        benchmark.load_data()
        benchmark.run_benchmark()
    finally:
        benchmark.cleanup()

if __name__ == "__main__":
    main() 