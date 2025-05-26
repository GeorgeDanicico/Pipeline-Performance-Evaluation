from fastapi import FastAPI, WebSocket, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import asyncio
import json
import httpx
from typing import Optional, Literal, List, Dict
from pydantic import BaseModel, Field
import os
from dotenv import load_dotenv
from benchmark.mongodb_benchmark import MongoDBBenchmark
from benchmark.couchbase_benchmark import CouchbaseBenchmark
from datetime import datetime

# Load environment variables
load_dotenv()

app = FastAPI()

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
WEBSOCKET_URL = os.getenv("WEBSOCKET_URL", "ws://localhost:8080/benchmark/ws")

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

class BenchmarkRequest(BaseModel):
    workloadType: str
    recordCount: int
    operationCount: int
    threadCount: int

class DatabaseMetrics(BaseModel):
    execution_time: int = Field(alias="executionTime")
    execution_p50: float = Field(alias="executionP50")
    execution_p75: float = Field(alias="executionP75")
    execution_p90: float = Field(alias="executionP90")
    execution_p99: float = Field(alias="executionP99")
    execute_operations_per_sec: float = Field(alias="executeOperationsPerSec")
    memory_usage: int = Field(alias="memoryUsage")
    index_memory_usage: int = Field(alias="indexMemoryUsage")
    loading_time: int = Field(alias="loadingTime")
    loading_p50: float = Field(alias="loadingP50")
    loading_p75: float = Field(alias="loadingP75")
    loading_p90: float = Field(alias="loadingP90")
    loading_p99: float = Field(alias="loadingP99")
    loading_operations_per_sec: float = Field(alias="loadingOperationsPerSec")
    loading_memory_usage: int = Field(alias="loadingMemoryUsage")
    loading_index_memory_usage: int = Field(alias="loadingIndexMemoryUsage")

    class Config:
        populate_by_name = True
        allow_population_by_field_name = True

class BenchmarkResponse(BaseModel):
    timestamp: float
    recordCount: int
    operationCount: int
    workloadType: str
    threadCount: int
    mongoMetrics: Optional[DatabaseMetrics]
    couchbaseMetrics: Optional[DatabaseMetrics]

class BenchmarkState:
    def __init__(self):
        self.mongodb_benchmark: Optional[MongoDBBenchmark] = None
        self.couchbase_benchmark: Optional[CouchbaseBenchmark] = None
        self.is_running = False
        self.current_database: Optional[str] = None

benchmark_state = BenchmarkState()

async def notify_backend(message: str):
    """Send WebSocket message to backend"""
    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(
                f"{BACKEND_URL}/api/benchmark/notify",
                json={"message": message}
            )
            response.raise_for_status()
        except Exception as e:
            print(f"Error notifying backend: {e}")

def format_metrics(benchmark, workload_name: str) -> Dict:
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

async def run_benchmark(benchmark, request: BenchmarkRequest) -> Dict:
    """Run benchmark for a specific database and return metrics"""
    try:
        # Update benchmark parameters
        benchmark.RECORD_COUNT = request.recordCount
        benchmark.OPERATION_COUNT = request.operationCount
        benchmark.THREAD_COUNTS = [request.threadCount]
        
        # Get the specific workload configuration
        workload_config = WORKLOAD_MAPPING[request.workloadType]
        
        # Initialize connection
        benchmark.init_connection()
        
        # Run load phase
        benchmark.load_data(
            thread_count=request.threadCount,
            record_count=request.recordCount
        )
        
        # Run benchmark phase with specific workload
        benchmark.run_benchmark(
            operation_count=request.operationCount,
            thread_count=request.threadCount,
            workload_config=workload_config
        )
        
        # Format metrics for the specific workload
        return format_metrics(benchmark, workload_config["name"])
        
    finally:
        benchmark.cleanup()

@app.post("/api/benchmark/start", response_model=BenchmarkResponse)
async def start_benchmark(request: BenchmarkRequest):
    """Start the benchmark for both databases sequentially"""
    if benchmark_state.is_running:
        raise HTTPException(status_code=400, detail="Benchmark is already running")
    
    if request.workloadType not in WORKLOAD_MAPPING:
        raise HTTPException(
            status_code=400, 
            detail=f"Invalid workload type. Must be one of: {', '.join(WORKLOAD_MAPPING.keys())}"
        )
    
    try:
        benchmark_state.is_running = True
        
        # Initialize benchmarks
        benchmark_state.mongodb_benchmark = MongoDBBenchmark()
        benchmark_state.couchbase_benchmark = CouchbaseBenchmark()
        
        # Run MongoDB benchmark first
        benchmark_state.current_database = "MongoDB"
        mongodb_metrics = await run_benchmark(benchmark_state.mongodb_benchmark, request)
        
        # Then run Couchbase benchmark
        benchmark_state.current_database = "Couchbase"
        couchbase_metrics = await run_benchmark(benchmark_state.couchbase_benchmark, request)
        
        # Create the response with consistent structure
        response = BenchmarkResponse(
            timestamp=datetime.now().timestamp(),
            recordCount=request.recordCount,
            operationCount=request.operationCount,
            workloadType=request.workloadType,
            threadCount=request.threadCount,
            mongoMetrics=mongodb_metrics,
            couchbaseMetrics=couchbase_metrics
        )
        
        return response
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
    finally:
        benchmark_state.is_running = False
        benchmark_state.current_database = None
        benchmark_state.mongodb_benchmark = None
        benchmark_state.couchbase_benchmark = None

@app.get("/api/benchmark/status")
async def get_benchmark_status():
    """Get the current status of the benchmark"""
    return {
        "is_running": benchmark_state.is_running,
        "current_database": benchmark_state.current_database
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000) 