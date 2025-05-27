from fastapi import FastAPI, WebSocket, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
import asyncio
import json
import httpx
from typing import Optional, List
from pydantic import BaseModel, Field
import os
from dotenv import load_dotenv
from datetime import datetime
from sqlalchemy.orm import Session
from database.database import get_db
from database.models import BenchmarkResult
from services.benchmark_service import BenchmarkService

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
    loading_memory_usage: int = 0
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

def get_benchmark_service(db: Session = Depends(get_db)) -> BenchmarkService:
    return BenchmarkService(db)

@app.post("/api/v1/benchmark/start", response_model=BenchmarkResponse)
async def start_benchmark(request: BenchmarkRequest, service: BenchmarkService = Depends(get_benchmark_service)):
    """Start the benchmark for both databases concurrently"""
    try:
        # Run benchmarks
        mongodb_metrics, couchbase_metrics = await service.start_benchmark(request)
        
        # Create the response
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
        saved_result = service.save_benchmark_result(db_result)
        response.id = saved_result.id
        
        return response
    
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/benchmark/status")
async def get_benchmark_status(service: BenchmarkService = Depends(get_benchmark_service)):
    """Get the current status of the benchmark"""
    return service.get_benchmark_status()

@app.get("/api/v1/histories", response_model=List[BenchmarkResponse])
async def get_benchmark_histories(service: BenchmarkService = Depends(get_benchmark_service)):
    """Get all benchmark history"""
    results = service.get_all_benchmark_results()
    
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
async def get_benchmark_history_by_id(history_id: int, service: BenchmarkService = Depends(get_benchmark_service)):
    """Get benchmark history by ID"""
    result = service.get_benchmark_result_by_id(history_id)
    
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