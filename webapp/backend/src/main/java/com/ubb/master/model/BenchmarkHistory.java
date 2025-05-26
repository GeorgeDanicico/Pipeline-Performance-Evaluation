package com.ubb.master.model;

import com.ubb.master.generated.api.model.WorkloadType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkHistory {
    private Long timestamp;

    private WorkloadType workloadType;

    private Long recordCount;
    private Long operationCount;
    private Long threadCount;
    private DatabaseMetrics mongoMetrics;
    private DatabaseMetrics couchbaseMetrics;
}