package com.ubb.master.model;

import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity
@Table(name = "results")
public class BenchmarkResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "operation_count")
    private Long operationCount;

    @Column(name = "thread_count")
    private Long threadCount;

    @Column(name = "timestamp")
    private Long timestamp;

    @Column(name = "workload_type")
    private String workloadType;

    // MongoDB execution metrics
    @Column(name = "mongo_execution_time")
    private Long mongoExecutionTime;

    @Column(name = "mongo_execution_p50")
    private Double mongoExecutionP50;

    @Column(name = "mongo_execution_p75")
    private Double mongoExecutionP75;

    @Column(name = "mongo_execution_p90")
    private Double mongoExecutionP90;

    @Column(name = "mongo_execution_p99")
    private Double mongoExecutionP99;

    @Column(name = "mongo_execute_operations_per_sec")
    private Double mongoExecuteOperationsPerSec;

    @Column(name = "mongo_memory_usage")
    private Long mongoMemoryUsage;

    @Column(name = "mongo_index_memory_usage")
    private Long mongoIndexMemoryUsage;

    // MongoDB loading metrics
    @Column(name = "mongo_loading_time")
    private Long mongoLoadingTime;

    @Column(name = "mongo_loading_p50")
    private Double mongoLoadingP50;

    @Column(name = "mongo_loading_p75")
    private Double mongoLoadingP75;

    @Column(name = "mongo_loading_p90")
    private Double mongoLoadingP90;

    @Column(name = "mongo_loading_p99")
    private Double mongoLoadingP99;

    @Column(name = "mongo_loading_operations_per_sec")
    private Double mongoLoadingOperationsPerSec;

    @Column(name = "mongo_loading_memory_usage")
    private Long mongoLoadingMemoryUsage;

    @Column(name = "mongo_loading_index_memory_usage")
    private Long mongoLoadingIndexMemoryUsage;

    // Couchbase execution metrics
    @Column(name = "couchbase_execution_time")
    private Long couchbaseExecutionTime;

    @Column(name = "couchbase_execution_p50")
    private Double couchbaseExecutionP50;

    @Column(name = "couchbase_execution_p75")
    private Double couchbaseExecutionP75;

    @Column(name = "couchbase_execution_p90")
    private Double couchbaseExecutionP90;

    @Column(name = "couchbase_execution_p99")
    private Double couchbaseExecutionP99;

    @Column(name = "couchbase_execute_operations_per_sec")
    private Double couchbaseExecuteOperationsPerSec;

    @Column(name = "couchbase_memory_usage")
    private Long couchbaseMemoryUsage;

    @Column(name = "couchbase_index_memory_usage")
    private Long couchbaseIndexMemoryUsage;

    // Couchbase loading metrics
    @Column(name = "couchbase_loading_time")
    private Long couchbaseLoadingTime;

    @Column(name = "couchbase_loading_p50")
    private Double couchbaseLoadingP50;

    @Column(name = "couchbase_loading_p75")
    private Double couchbaseLoadingP75;

    @Column(name = "couchbase_loading_p90")
    private Double couchbaseLoadingP90;

    @Column(name = "couchbase_loading_p99")
    private Double couchbaseLoadingP99;

    @Column(name = "couchbase_loading_operations_per_sec")
    private Double couchbaseLoadingOperationsPerSec;

    @Column(name = "couchbase_loading_memory_usage")
    private Long couchbaseLoadingMemoryUsage;

    @Column(name = "couchbase_loading_index_memory_usage")
    private Long couchbaseLoadingIndexMemoryUsage;
}