from typing import Dict, List, Optional, Tuple
from datetime import datetime
from sqlalchemy.orm import Session
from database.repository import BenchmarkRepository
from database.models import BenchmarkResult
from benchmark.mongodb_benchmark import MongoDBBenchmark
from benchmark.couchbase_benchmark import CouchbaseBenchmark
import asyncio
from main import manager

class BenchmarkService:
    def __init__(self, db: Session):
        self.db = db
        self.repository = BenchmarkRepository(db)
        self.mongodb_benchmark: Optional[MongoDBBenchmark] = None
        self.couchbase_benchmark: Optional[CouchbaseBenchmark] = None
        self.is_running = False
        self.current_database: Optional[str] = None

    WORKLOAD_MAPPING = {
        "WORKLOAD_A": {
            "name": "Workload A",
            "read_proportion": 0.5,
            "update_proportion": 0.5
        },
        "WORKLOAD_B": {
            "name": "Workload B",
            "read_proportion": 0.95,
            "update_proportion": 0.05
        },
        "WORKLOAD_C": {
            "name": "Workload C",
            "read_proportion": 1.0,
            "update_proportion": 0.0
        },
        "WORKLOAD_D": {
            "name": "Workload D",
            "read_proportion": 0.05,
            "update_proportion": 0.95
        }
    }

    def format_metrics(self, benchmark, workload_name: str) -> Dict:
        """Format benchmark metrics to match BenchmarkHistory structure"""
        # Get the latencies for the current workload
        read_latencies = benchmark.read_latencies
        update_latencies = benchmark.update_latencies
        insert_latencies = benchmark.insert_latencies

        # Calculate execution metrics (run phase)
        execution_time = benchmark.run_phase_end_time - benchmark.run_phase_start_time
        total_operations = benchmark.read_counter + benchmark.update_counter
        execute_ops_per_sec = total_operations / execution_time if execution_time > 0 else 0

        # Calculate loading metrics (load phase)
        loading_time = benchmark.load_phase_end_time - benchmark.load_phase_start_time
        loading_ops_per_sec = benchmark.RECORD_COUNT / loading_time if loading_time > 0 else 0

        # Get resource usage
        memory_usage = max(benchmark.ram_usage) if benchmark.ram_usage else 0
        index_memory_usage = 0  # This would need to be implemented specifically for each database

        # Calculate loading percentiles
        loading_percentiles = benchmark.calculate_percentiles(insert_latencies)

        # Create database metrics object
        database_metrics = {
            "executionTime": int(execution_time * 1000),  # Convert to milliseconds
            "executionP50": benchmark.calculate_percentiles(read_latencies + update_latencies).get("p50", 0) * 1000,
            "executionP75": benchmark.calculate_percentiles(read_latencies + update_latencies).get("p75", 0) * 1000,
            "executionP90": benchmark.calculate_percentiles(read_latencies + update_latencies).get("p90", 0) * 1000,
            "executionP99": benchmark.calculate_percentiles(read_latencies + update_latencies).get("p99", 0) * 1000,
            "executeOperationsPerSec": execute_ops_per_sec,
            "memoryUsage": int(memory_usage),
            "indexMemoryUsage": index_memory_usage,
            "loadingTime": int(loading_time * 1000),
            "loadingP50": loading_percentiles.get("p50", 0) * 1000,
            "loadingP75": loading_percentiles.get("p75", 0) * 1000,
            "loadingP90": loading_percentiles.get("p90", 0) * 1000,
            "loadingP99": loading_percentiles.get("p99", 0) * 1000,
            "loadingOperationsPerSec": loading_ops_per_sec,
            "loadingMemoryUsage": int(memory_usage),  # Using same memory usage for loading
            "loadingIndexMemoryUsage": index_memory_usage
        }

        return database_metrics

    async def run_benchmark(self, benchmark, request) -> Dict:
        """Run benchmark for a specific database and return metrics"""
        try:
            # Update benchmark parameters
            benchmark.RECORD_COUNT = request.recordCount
            benchmark.OPERATION_COUNT = request.operationCount
            benchmark.THREAD_COUNTS = [request.threadCount]
            
            # Get the specific workload configuration
            workload_config = self.WORKLOAD_MAPPING[request.workloadType]
            
            # Initialize connection
            benchmark.init_connection()
            
            # Run load phase
            benchmark.load_data(
                thread_count=request.threadCount,
                record_count=request.recordCount
            )
            
            # Notify load phase complete
            await manager.broadcast({
                "type": "load_complete",
                "message": f"Loading phase completed for {self.current_database}"
            })
            
            # Run benchmark phase with specific workload
            benchmark.run_benchmark(
                operation_count=request.operationCount,
                thread_count=request.threadCount,
                workload_config=workload_config
            )
            
            # Notify benchmark phase complete
            await manager.broadcast({
                "type": "benchmark_complete",
                "message": f"Benchmark phase completed for {self.current_database}"
            })
            
            # Format metrics for the specific workload
            return self.format_metrics(benchmark, workload_config["name"])
            
        finally:
            benchmark.cleanup()

    async def start_benchmark(self, request) -> Tuple[Dict, Dict]:
        """Start the benchmark for both databases synchronously"""
        if self.is_running:
            raise ValueError("Benchmark is already running")
        
        if request.workloadType not in self.WORKLOAD_MAPPING:
            raise ValueError(f"Invalid workload type. Must be one of: {', '.join(self.WORKLOAD_MAPPING.keys())}")
        
        try:
            self.is_running = True
            
            # Initialize benchmarks
            self.mongodb_benchmark = MongoDBBenchmark()
            self.couchbase_benchmark = CouchbaseBenchmark()
            
            # Run benchmarks synchronously
            self.current_database = "MongoDB"
            mongodb_metrics = await self.run_benchmark(self.mongodb_benchmark, request)
            
            self.current_database = "Couchbase"
            couchbase_metrics = await self.run_benchmark(self.couchbase_benchmark, request)
            
            return mongodb_metrics, couchbase_metrics
            
        finally:
            self.is_running = False
            self.current_database = None
            self.mongodb_benchmark = None
            self.couchbase_benchmark = None

    def get_benchmark_status(self) -> Dict:
        """Get the current status of the benchmark"""
        return {
            "is_running": self.is_running,
            "current_database": self.current_database
        }

    def get_all_benchmark_results(self) -> List[BenchmarkResult]:
        """Get all benchmark results"""
        return self.repository.get_all_benchmark_results()

    def get_benchmark_result_by_id(self, result_id: int) -> Optional[BenchmarkResult]:
        """Get benchmark result by ID"""
        return self.repository.get_benchmark_result_by_id(result_id)

    def save_benchmark_result(self, result: BenchmarkResult) -> BenchmarkResult:
        """Save benchmark result to database"""
        return self.repository.create_benchmark_result(result) 