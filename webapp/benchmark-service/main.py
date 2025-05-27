from fastapi import FastAPI, WebSocket, HTTPException, Depends, WebSocketDisconnect
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
from sqlalchemy.orm import Session
from database.database import get_db
from database.repository import BenchmarkRepository
from database.models import BenchmarkResult

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

# WebSocket connection manager
class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        for connection in self.active_connections:
            await connection.send_json(message)

manager = ConnectionManager()

# WebSocket endpoint
@app.websocket("/api/ws/benchmark")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            # Handle incoming messages if needed
    except WebSocketDisconnect:
        manager.disconnect(websocket)

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
    loading_memory_usage: int =0
    loading_index_memory_usage: int = 0

    class Config:
        populate_by_name = True
        allow_population_by_field_name = True

class BenchmarkResponse(BaseModel):
    id: Optional[int] = None
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
        
        # Notify load phase complete
        await manager.broadcast({
            "type": "load_complete",
            "message": f"Loading phase completed for {benchmark.__class__.__name__.replace('Benchmark', '')}"
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
            "message": f"Benchmark phase completed for {benchmark.__class__.__name__.replace('Benchmark', '')}"
        })
        
        # Format metrics for the specific workload
        return format_metrics(benchmark, workload_config["name"])
        
    finally:
        benchmark.cleanup()

@app.post("/api/v1/benchmark/start", response_model=BenchmarkResponse)
async def start_benchmark(request: BenchmarkRequest, db: Session = Depends(get_db)):
    """Start the benchmark for both databases concurrently"""
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
        
        # Run benchmarks concurrently
        benchmark_state.current_database = "Both"
        mongodb_task = asyncio.create_task(run_benchmark(benchmark_state.mongodb_benchmark, request))
        couchbase_task = asyncio.create_task(run_benchmark(benchmark_state.couchbase_benchmark, request))
        
        # Wait for both benchmarks to complete
        mongodb_metrics, couchbase_metrics = await asyncio.gather(mongodb_task, couchbase_task)
        
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

        # Save to database
        repository = BenchmarkRepository(db)
        db_result = BenchmarkResult(
            timestamp=response.timestamp,
            workload_type=response.workloadType,
            record_count=response.recordCount,
            operation_count=response.operationCount,
            thread_count=response.threadCount,
            # MongoDB metrics
            mongo_execution_time=response.mongoMetrics.execution_time if response.mongoMetrics else None,
            mongo_execution_p50=response.mongoMetrics.execution_p50 if response.mongoMetrics else None,
            mongo_execution_p75=response.mongoMetrics.execution_p75 if response.mongoMetrics else None,
            mongo_execution_p90=response.mongoMetrics.execution_p90 if response.mongoMetrics else None,
            mongo_execution_p99=response.mongoMetrics.execution_p99 if response.mongoMetrics else None,
            mongo_execute_operations_per_sec=response.mongoMetrics.execute_operations_per_sec if response.mongoMetrics else None,
            mongo_memory_usage=response.mongoMetrics.memory_usage if response.mongoMetrics else None,
            mongo_index_memory_usage=response.mongoMetrics.index_memory_usage if response.mongoMetrics else None,
            mongo_loading_time=response.mongoMetrics.loading_time if response.mongoMetrics else None,
            mongo_loading_p50=response.mongoMetrics.loading_p50 if response.mongoMetrics else None,
            mongo_loading_p75=response.mongoMetrics.loading_p75 if response.mongoMetrics else None,
            mongo_loading_p90=response.mongoMetrics.loading_p90 if response.mongoMetrics else None,
            mongo_loading_p99=response.mongoMetrics.loading_p99 if response.mongoMetrics else None,
            mongo_loading_operations_per_sec=response.mongoMetrics.loading_operations_per_sec if response.mongoMetrics else None,
            mongo_loading_memory_usage=response.mongoMetrics.loading_memory_usage if response.mongoMetrics else None,
            mongo_loading_index_memory_usage=response.mongoMetrics.loading_index_memory_usage if response.mongoMetrics else None,
            # Couchbase metrics
            couchbase_execution_time=response.couchbaseMetrics.execution_time if response.couchbaseMetrics else None,
            couchbase_execution_p50=response.couchbaseMetrics.execution_p50 if response.couchbaseMetrics else None,
            couchbase_execution_p75=response.couchbaseMetrics.execution_p75 if response.couchbaseMetrics else None,
            couchbase_execution_p90=response.couchbaseMetrics.execution_p90 if response.couchbaseMetrics else None,
            couchbase_execution_p99=response.couchbaseMetrics.execution_p99 if response.couchbaseMetrics else None,
            couchbase_execute_operations_per_sec=response.couchbaseMetrics.execute_operations_per_sec if response.couchbaseMetrics else None,
            couchbase_memory_usage=response.couchbaseMetrics.memory_usage if response.couchbaseMetrics else None,
            couchbase_index_memory_usage=response.couchbaseMetrics.index_memory_usage if response.couchbaseMetrics else None,
            couchbase_loading_time=response.couchbaseMetrics.loading_time if response.couchbaseMetrics else None,
            couchbase_loading_p50=response.couchbaseMetrics.loading_p50 if response.couchbaseMetrics else None,
            couchbase_loading_p75=response.couchbaseMetrics.loading_p75 if response.couchbaseMetrics else None,
            couchbase_loading_p90=response.couchbaseMetrics.loading_p90 if response.couchbaseMetrics else None,
            couchbase_loading_p99=response.couchbaseMetrics.loading_p99 if response.couchbaseMetrics else None,
            couchbase_loading_operations_per_sec=response.couchbaseMetrics.loading_operations_per_sec if response.couchbaseMetrics else None,
            couchbase_loading_memory_usage=response.couchbaseMetrics.loading_memory_usage if response.couchbaseMetrics else None,
            couchbase_loading_index_memory_usage=response.couchbaseMetrics.loading_index_memory_usage if response.couchbaseMetrics else None,
        )
        saved_result = repository.create_benchmark_result(db_result)
        response.id = saved_result.id
        
        return response
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
    finally:
        benchmark_state.is_running = False
        benchmark_state.current_database = None
        benchmark_state.mongodb_benchmark = None
        benchmark_state.couchbase_benchmark = None

@app.get("/api/v1/benchmark/status")
async def get_benchmark_status():
    """Get the current status of the benchmark"""
    return {
        "is_running": benchmark_state.is_running,
        "current_database": benchmark_state.current_database
    }

@app.get("/api/v1/histories", response_model=List[BenchmarkResponse])
async def get_benchmark_history(db: Session = Depends(get_db)):
    """Get all benchmark history"""
    repository = BenchmarkRepository(db)
    results = repository.get_all_benchmark_results()
    
    return [
        BenchmarkResponse(
            id=result.id,
            timestamp=result.timestamp,
            recordCount=result.record_count,
            operationCount=result.operation_count,
            workloadType=result.workload_type,
            threadCount=result.thread_count,
            mongoMetrics=DatabaseMetrics(
                executionTime=result.mongo_execution_time,
                executionP50=result.mongo_execution_p50,
                executionP75=result.mongo_execution_p75,
                executionP90=result.mongo_execution_p90,
                executionP99=result.mongo_execution_p99,
                executeOperationsPerSec=result.mongo_execute_operations_per_sec,
                memoryUsage=result.mongo_memory_usage,
                indexMemoryUsage=result.mongo_index_memory_usage,
                loadingTime=result.mongo_loading_time,
                loadingP50=result.mongo_loading_p50,
                loadingP75=result.mongo_loading_p75,
                loadingP90=result.mongo_loading_p90,
                loadingP99=result.mongo_loading_p99,
                loadingOperationsPerSec=result.mongo_loading_operations_per_sec,
                loadingMemoryUsage=result.mongo_loading_memory_usage,
                loadingIndexMemoryUsage=result.mongo_loading_index_memory_usage
            ) if result.mongo_execution_time else None,
            couchbaseMetrics=DatabaseMetrics(
                executionTime=result.couchbase_execution_time,
                executionP50=result.couchbase_execution_p50,
                executionP75=result.couchbase_execution_p75,
                executionP90=result.couchbase_execution_p90,
                executionP99=result.couchbase_execution_p99,
                executeOperationsPerSec=result.couchbase_execute_operations_per_sec,
                memoryUsage=result.couchbase_memory_usage,
                indexMemoryUsage=result.couchbase_index_memory_usage,
                loadingTime=result.couchbase_loading_time,
                loadingP50=result.couchbase_loading_p50,
                loadingP75=result.couchbase_loading_p75,
                loadingP90=result.couchbase_loading_p90,
                loadingP99=result.couchbase_loading_p99,
                loadingOperationsPerSec=result.couchbase_loading_operations_per_sec,
                loadingMemoryUsage=result.couchbase_loading_memory_usage,
                loadingIndexMemoryUsage=result.couchbase_loading_index_memory_usage
            ) if result.couchbase_execution_time else None
        )
        for result in results
    ]

@app.get("/api/v1/histories/{history_id}", response_model=BenchmarkResponse)
async def get_benchmark_history_by_id(history_id: int, db: Session = Depends(get_db)):
    """Get benchmark history by ID"""
    repository = BenchmarkRepository(db)
    result = repository.get_benchmark_result_by_id(history_id)
    
    if not result:
        raise HTTPException(status_code=404, detail="Benchmark history not found")
    
    return BenchmarkResponse(
        id=result.id,
        timestamp=result.timestamp,
        recordCount=result.record_count,
        operationCount=result.operation_count,
        workloadType=result.workload_type,
        threadCount=result.thread_count,
        mongoMetrics=DatabaseMetrics(
            executionTime=result.mongo_execution_time,
            executionP50=result.mongo_execution_p50,
            executionP75=result.mongo_execution_p75,
            executionP90=result.mongo_execution_p90,
            executionP99=result.mongo_execution_p99,
            executeOperationsPerSec=result.mongo_execute_operations_per_sec,
            memoryUsage=result.mongo_memory_usage,
            indexMemoryUsage=result.mongo_index_memory_usage,
            loadingTime=result.mongo_loading_time,
            loadingP50=result.mongo_loading_p50,
            loadingP75=result.mongo_loading_p75,
            loadingP90=result.mongo_loading_p90,
            loadingP99=result.mongo_loading_p99,
            loadingOperationsPerSec=result.mongo_loading_operations_per_sec,
            loadingMemoryUsage=result.mongo_loading_memory_usage,
            loadingIndexMemoryUsage=result.mongo_loading_index_memory_usage
        ) if result.mongo_execution_time else None,
        couchbaseMetrics=DatabaseMetrics(
            executionTime=result.couchbase_execution_time,
            executionP50=result.couchbase_execution_p50,
            executionP75=result.couchbase_execution_p75,
            executionP90=result.couchbase_execution_p90,
            executionP99=result.couchbase_execution_p99,
            executeOperationsPerSec=result.couchbase_execute_operations_per_sec,
            memoryUsage=result.couchbase_memory_usage,
            indexMemoryUsage=result.couchbase_index_memory_usage,
            loadingTime=result.couchbase_loading_time,
            loadingP50=result.couchbase_loading_p50,
            loadingP75=result.couchbase_loading_p75,
            loadingP90=result.couchbase_loading_p90,
            loadingP99=result.couchbase_loading_p99,
            loadingOperationsPerSec=result.couchbase_loading_operations_per_sec,
            loadingMemoryUsage=result.couchbase_loading_memory_usage,
            loadingIndexMemoryUsage=result.couchbase_loading_index_memory_usage
        ) if result.couchbase_execution_time else None
    )

if __name__ == "__main__":
    import uvicorn
    from database.init_db import init_db
    
    # Initialize database
    init_db()
    
    # Start the application
    uvicorn.run(app, host="0.0.0.0", port=8000) 