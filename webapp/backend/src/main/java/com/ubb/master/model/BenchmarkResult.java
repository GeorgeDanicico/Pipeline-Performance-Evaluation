package com.ubb.master.model;

import lombok.Data;

import jakarta.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "results")
public class BenchmarkResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "database")
    private String database;

    @Column(name = "execution_time")
    private long executionTime;

    @Column(name = "execution_p50")
    private double executionP50;

    @Column(name = "execution_p75")
    private double executionP75;

    @Column(name = "execution_p90")
    private double executionP90;

    @Column(name = "execution_p99")
    private double executionP99;

    @Column(name = "execute_operations_per_sec")
    private double executeOperationsPerSec;

    @Column(name = "memory_usage")
    private long memoryUsage;

    @Column(name = "index_memory_usage")
    private long indexMemoryUsage;

    // Loading metrics
    @Column(name = "loading_time")
    private long loadingTime;

    @Column(name = "loading_p50")
    private double loadingP50;

    @Column(name = "loading_p75")
    private double loadingP75;

    @Column(name = "loading_p90")
    private double loadingP90;

    @Column(name = "loading_p99")
    private double loadingP99;

    @Column(name = "loading_operations_per_sec")
    private double loadingOperationsPerSec;
}