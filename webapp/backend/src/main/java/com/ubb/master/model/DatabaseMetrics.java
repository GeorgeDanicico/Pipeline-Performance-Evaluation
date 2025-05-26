package com.ubb.master.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseMetrics {
    private Long executionTime;
    private Double executionP50;
    private Double executionP75;
    private Double executionP90;
    private Double executionP99;
    private Double executeOperationsPerSec;
    private Long memoryUsage;
    private Long indexMemoryUsage;

    private Long loadingTime;
    private Double loadingP50;
    private Double loadingP75;
    private Double loadingP90;
    private Double loadingP99;
    private Double loadingOperationsPerSec;
    private Long loadingMemoryUsage;
    private Long loadingIndexMemoryUsage;
}