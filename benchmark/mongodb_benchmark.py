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
import threading

class MongoDBBenchmark:
    def __init__(self):
        # Configuration constants
        self.HOST = "127.0.0.1"
        self.DATABASE_NAME = "ycsb"
        self.COLLECTION_NAME = "usertable"
        self.RECORD_COUNT = 10000
        self.OPERATION_COUNT = 10000
        self.THREAD_COUNTS = [1, 2, 4]  # List of thread counts to test

        # Define workload configurations
        self.WORKLOADS = [
            {
                "name": "Balanced",
                "read_proportion": 0.5,
                "update_proportion": 0.5,
                "insert_proportion": 0,
                "scan_proportion": 0
            },
            {
                "name": "Read-Heavy",
                "read_proportion": 0.95,
                "update_proportion": 0.05,
                "insert_proportion": 0,
                "scan_proportion": 0
            },
            {
                "name": "Read-Only",
                "read_proportion": 1.0,
                "update_proportion": 0.0,
                "insert_proportion": 0,
                "scan_proportion": 0
            },
            {
                "name": "Update-Heavy",
                "read_proportion": 0.05,
                "update_proportion": 0.95,
                "insert_proportion": 0,
                "scan_proportion": 0
            }
        ]

        # Metrics
        self.read_counter = 0
        self.update_counter = 0
        self.insert_counter = 0
        self.scan_counter = 0
        self.error_counter = 0

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
        self.client = MongoClient(f"mongodb://{self.HOST}:27017?retryWrites=false")
        self.db = self.client[self.DATABASE_NAME]
        self.collection = self.db[self.COLLECTION_NAME]

    def cleanup(self):
        """Cleanup MongoDB connection"""
        if self.client:
            self.client.close()

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

    def load_data(self):
        """Load initial data into MongoDB"""
        print("Starting load phase...")
        self.load_phase_start_time = time.time()
        self.start_monitoring()

        def load_worker(start_record: int, end_record: int):
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
                except Exception as e:
                    print(f"Error inserting record {key}: {e}")
                    self.error_counter += 1

        records_per_thread = self.RECORD_COUNT // self.THREAD_COUNTS[0]
        threads = []

        for i in range(self.THREAD_COUNTS[0]):
            start_record = i * records_per_thread
            end_record = self.RECORD_COUNT if i == self.THREAD_COUNTS[0] - 1 else (i + 1) * records_per_thread
            thread = threading.Thread(target=load_worker, args=(start_record, end_record))
            threads.append(thread)
            thread.start()

        for thread in threads:
            thread.join()

        self.load_phase_end_time = time.time()
        self.stop_monitoring()
        print("Load phase completed.")
        
        # Print load phase metrics
        self.print_phase_metrics(
            "Load",
            self.load_phase_start_time,
            self.load_phase_end_time,
            self.RECORD_COUNT,
            self.insert_latencies
        )
        self.print_resource_metrics("Load")

    def run_benchmark(self):
        """Run the benchmark operations for all workloads and thread counts"""
        print("\nStarting benchmark runs for all workloads and thread counts...")
        
        for thread_count in self.THREAD_COUNTS:
            print(f"\n=== Testing with {thread_count} threads ===")
            
            for workload in self.WORKLOADS:
                print(f"\n=== Starting {workload['name']} Workload ===")
                print(f"Read proportion: {workload['read_proportion']}")
                print(f"Update proportion: {workload['update_proportion']}")
                
                # Reset counters and latencies for this workload
                self.read_counter = 0
                self.update_counter = 0
                self.insert_counter = 0
                self.scan_counter = 0
                self.error_counter = 0
                self.read_latencies = []
                self.update_latencies = []
                self.insert_latencies = []
                self.scan_latencies = []
                
                self.run_phase_start_time = time.time()
                self.start_monitoring()

                def run_worker():
                    for _ in range(self.OPERATION_COUNT // thread_count):
                        key = f"user{hash(str(time.time())) % self.RECORD_COUNT}"
                        random_value = time.time() % 1.0

                        if random_value < workload['read_proportion']:
                            # Read operation
                            start_time = time.time()
                            try:
                                result = self.collection.find_one({"_id": key})
                                self.read_latencies.append(time.time() - start_time)
                                self.read_counter += 1
                            except Exception as e:
                                print(f"Error reading record {key}: {e}")
                                self.error_counter += 1

                        elif random_value < workload['read_proportion'] + workload['update_proportion']:
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
                print(f"\n{workload['name']} workload completed.")

                # Print run phase metrics for this workload
                print(f"\n=== {workload['name']} Workload Results ===")
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

                self.print_resource_metrics(f"{workload['name']} Run")

                # Save metrics for this workload and thread count
                self.save_metrics(workload['name'], thread_count)

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