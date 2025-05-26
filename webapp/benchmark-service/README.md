# Benchmark Service

This service provides a FastAPI interface to run MongoDB and Couchbase benchmarks and communicate the results to the Quarkus backend.

## Setup

1. Install the required dependencies:
```bash
pip install -r requirements.txt
```

2. Make sure MongoDB is running on localhost:27017
   - Database name: ycsb
   - Collection name: usertable

3. Make sure Couchbase is running on localhost:8091
   - Bucket name: census
   - Bucket password: parola03

4. Make sure the Quarkus backend is running on localhost:8080

## Running the Service

Start the service with:
```bash
python main.py
```

The service will run on http://localhost:8000

## API Endpoints

### Start Benchmark
- **POST** `/api/benchmark/start`
- Starts the benchmark for the specified database with given parameters
- Request body:
```json
{
    "database": "mongodb" | "couchbase",
    "record_count": 1000000,  // Number of records to load
    "operation_count": 1000000,  // Number of operations to perform
    "thread_count": 4  // Number of threads to use
}
```
- Returns success message when complete

### Get Status
- **GET** `/api/benchmark/status`
- Returns the current status of the benchmark
- Response: `{"is_running": boolean, "current_database": string | null}`

## Communication Flow

1. Frontend calls `/api/benchmark/start` with the database type and parameters to start the benchmark
2. Service runs the load phase with the specified record count
3. Service notifies backend via HTTP POST when load phase is complete with message `{database}_LOAD_PHASE_COMPLETE`
4. Service runs the benchmark phase with the specified operation count and thread count
5. Service sends results to backend when complete with message `{database}_BENCHMARK_COMPLETE`
6. Backend forwards results to frontend

## Metrics

The service saves detailed metrics for each benchmark run in the `metrics` directory. The metrics include:
- Load phase duration
- Run phase duration
- Read/Update/Insert/Scan latencies
- Operation counts
- Resource usage (CPU/RAM) 