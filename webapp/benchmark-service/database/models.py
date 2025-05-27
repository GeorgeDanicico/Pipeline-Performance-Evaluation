from sqlalchemy import Column, Integer, Float, String, DateTime
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime

Base = declarative_base()

class BenchmarkResult(Base):
    __tablename__ = "results"

    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(Float)
    workload_type = Column(String)
    record_count = Column(Integer)
    operation_count = Column(Integer)
    thread_count = Column(Integer)

    # MongoDB execution metrics
    mongo_execution_time = Column(Integer)
    mongo_execution_p50 = Column(Float)
    mongo_execution_p75 = Column(Float)
    mongo_execution_p90 = Column(Float)
    mongo_execution_p99 = Column(Float)
    mongo_execute_operations_per_sec = Column(Float)
    mongo_memory_usage = Column(Integer)
    mongo_index_memory_usage = Column(Integer)

    # MongoDB loading metrics
    mongo_loading_time = Column(Integer)
    mongo_loading_p50 = Column(Float)
    mongo_loading_p75 = Column(Float)
    mongo_loading_p90 = Column(Float)
    mongo_loading_p99 = Column(Float)
    mongo_loading_operations_per_sec = Column(Float)
    mongo_loading_memory_usage = Column(Integer)
    mongo_loading_index_memory_usage = Column(Integer)

    # Couchbase execution metrics
    couchbase_execution_time = Column(Integer)
    couchbase_execution_p50 = Column(Float)
    couchbase_execution_p75 = Column(Float)
    couchbase_execution_p90 = Column(Float)
    couchbase_execution_p99 = Column(Float)
    couchbase_execute_operations_per_sec = Column(Float)
    couchbase_memory_usage = Column(Integer)
    couchbase_index_memory_usage = Column(Integer)

    # Couchbase loading metrics
    couchbase_loading_time = Column(Integer)
    couchbase_loading_p50 = Column(Float)
    couchbase_loading_p75 = Column(Float)
    couchbase_loading_p90 = Column(Float)
    couchbase_loading_p99 = Column(Float)
    couchbase_loading_operations_per_sec = Column(Float)
    couchbase_loading_memory_usage = Column(Integer)
    couchbase_loading_index_memory_usage = Column(Integer) 