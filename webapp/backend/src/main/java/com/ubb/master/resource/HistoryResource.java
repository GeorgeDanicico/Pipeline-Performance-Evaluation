package com.ubb.master.resource;

import com.ubb.master.generated.api.HistoryApi;
import com.ubb.master.generated.api.model.BenchmarkHistory;
import com.ubb.master.generated.api.model.DatabaseMetrics;
import com.ubb.master.generated.api.model.WorkloadType;

import com.ubb.master.service.BenchmarkService;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HistoryResource implements HistoryApi {

    private final Map<Long, BenchmarkHistory> mockHistories = new HashMap<>();
    private final BenchmarkService benchmarkService;

    public HistoryResource(BenchmarkService benchmarkService) {
        initializeMockData();
        this.benchmarkService = benchmarkService;
    }

    private void initializeMockData() {
        // Create 5 mock benchmark histories
        for (long i = 1; i <= 5; i++) {
            mockHistories.put(i, createMockBenchmarkHistory(i));
        }
    }

    private BenchmarkHistory createMockBenchmarkHistory(long id) {
        BenchmarkHistory history = new BenchmarkHistory();
        history.setId(id);
        history.setTimestamp(0L);
        history.setWorkloadType(getRandomWorkloadType(id));
        history.setRecordCount(100000L * id);
        history.setOperationCount(50000L * id);
        history.setThreadCount(4L * (id % 3 + 1));

        // Create mock metrics for MongoDB
        history.setMongoMetrics(createMockDatabaseMetrics(100 * id));

        // Create mock metrics for Couchbase with slightly different values
        history.setCouchbaseMetrics(createMockDatabaseMetrics(120 * id));

        return history;
    }

    private DatabaseMetrics createMockDatabaseMetrics(double baseValue) {
        DatabaseMetrics metrics = new DatabaseMetrics();
        metrics.setExecutionTime((long) (baseValue * 10));
        metrics.setExecutionP50(baseValue * 0.5);
        metrics.setExecutionP75(baseValue * 0.75);
        metrics.setExecutionP90(baseValue * 0.9);
        metrics.setExecutionP99(baseValue * 0.99);
        metrics.setExecuteOperationsPerSec(baseValue * 100);
        metrics.setMemoryUsage((long) (baseValue * 1024));
        metrics.setIndexMemoryUsage((long) (baseValue * 512));

        metrics.setLoadingTime((long) (baseValue * 5));
        metrics.setLoadingP50(baseValue * 0.4);
        metrics.setLoadingP75(baseValue * 0.65);
        metrics.setLoadingP90(baseValue * 0.85);
        metrics.setLoadingP99(baseValue * 0.95);
        metrics.setLoadingOperationsPerSec(baseValue * 80);
        metrics.setLoadingMemoryUsage((long) (baseValue * 768));
        metrics.setLoadingIndexMemoryUsage((long) (baseValue * 384));

        return metrics;
    }

    private WorkloadType getRandomWorkloadType(long seed) {
        WorkloadType[] types = WorkloadType.values();
        return types[(int) (seed % types.length)];
    }

    @Override
    public List<BenchmarkHistory> getBenchmarkHistories() {
        return benchmarkService.getBenchmarkHistory();
    }

    @Override
    public BenchmarkHistory getBenchmarkHistoryById(Long id) {
        return benchmarkService.getBenchmarkHistoryById(id);
    }
}