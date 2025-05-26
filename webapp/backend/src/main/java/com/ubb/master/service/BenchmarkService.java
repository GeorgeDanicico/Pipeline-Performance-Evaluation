package com.ubb.master.service;

import com.ubb.master.client.PythonClient;
import com.ubb.master.generated.api.model.BenchmarkHistory;
import com.ubb.master.generated.api.model.BenchmarkRequest;
import com.ubb.master.generated.api.model.DatabaseMetrics;
import com.ubb.master.generated.api.model.WorkloadType;
import com.ubb.master.model.BenchmarkResult;
import com.ubb.master.repository.BenchmarkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BenchmarkService {
    @RestClient
    PythonClient pythonClient;
    private final BenchmarkRepository benchmarkRepository;

    public BenchmarkService(BenchmarkRepository benchmarkRepository) {
        this.benchmarkRepository = benchmarkRepository;
    }

    public BenchmarkResult startBenchmark(BenchmarkRequest benchmarkRequest) {
        var benchmarkResponse = pythonClient.startBenchmark(benchmarkRequest);

        // Create a single result record with both MongoDB and Couchbase metrics
        BenchmarkResult result = new BenchmarkResult();
        result.setTimestamp(benchmarkResponse.getTimestamp());
        result.setRecordCount(benchmarkResponse.getRecordCount());
        result.setOperationCount(benchmarkResponse.getOperationCount() != null ? benchmarkResponse.getOperationCount() : 0L);

        // Set MongoDB metrics if available
        if (benchmarkResponse.getMongoMetrics() != null) {
            // Store MongoDB metrics with "mongo_" prefix (handled by entity mapping)
            result.setMongoExecutionTime(benchmarkResponse.getMongoMetrics().getExecutionTime());
            result.setMongoExecutionP50(benchmarkResponse.getMongoMetrics().getExecutionP50());
            result.setMongoExecutionP75(benchmarkResponse.getMongoMetrics().getExecutionP75());
            result.setMongoExecutionP90(benchmarkResponse.getMongoMetrics().getExecutionP90());
            result.setMongoExecutionP99(benchmarkResponse.getMongoMetrics().getExecutionP99());
            result.setMongoExecuteOperationsPerSec(benchmarkResponse.getMongoMetrics().getExecuteOperationsPerSec());
            result.setMongoMemoryUsage(benchmarkResponse.getMongoMetrics().getMemoryUsage());
            result.setMongoIndexMemoryUsage(benchmarkResponse.getMongoMetrics().getIndexMemoryUsage());

            result.setMongoLoadingTime(benchmarkResponse.getMongoMetrics().getLoadingTime());
            result.setMongoLoadingP50(benchmarkResponse.getMongoMetrics().getLoadingP50());
            result.setMongoLoadingP75(benchmarkResponse.getMongoMetrics().getLoadingP75());
            result.setMongoLoadingP90(benchmarkResponse.getMongoMetrics().getLoadingP90());
            result.setMongoLoadingP99(benchmarkResponse.getMongoMetrics().getLoadingP99());
            result.setMongoLoadingOperationsPerSec(benchmarkResponse.getMongoMetrics().getLoadingOperationsPerSec());
        }

        // Set Couchbase metrics if available
        if (benchmarkResponse.getCouchbaseMetrics() != null) {
            // Store Couchbase metrics with "couchbase_" prefix (handled by entity mapping)
            result.setCouchbaseExecutionTime(benchmarkResponse.getCouchbaseMetrics().getExecutionTime());
            result.setCouchbaseExecutionP50(benchmarkResponse.getCouchbaseMetrics().getExecutionP50());
            result.setCouchbaseExecutionP75(benchmarkResponse.getCouchbaseMetrics().getExecutionP75());
            result.setCouchbaseExecutionP90(benchmarkResponse.getCouchbaseMetrics().getExecutionP90());
            result.setCouchbaseExecutionP99(benchmarkResponse.getCouchbaseMetrics().getExecutionP99());
            result.setCouchbaseExecuteOperationsPerSec(benchmarkResponse.getCouchbaseMetrics().getExecuteOperationsPerSec());
            result.setCouchbaseMemoryUsage(benchmarkResponse.getCouchbaseMetrics().getMemoryUsage());
            result.setCouchbaseIndexMemoryUsage(benchmarkResponse.getCouchbaseMetrics().getIndexMemoryUsage());

            result.setCouchbaseLoadingTime(benchmarkResponse.getCouchbaseMetrics().getLoadingTime());
            result.setCouchbaseLoadingP50(benchmarkResponse.getCouchbaseMetrics().getLoadingP50());
            result.setCouchbaseLoadingP75(benchmarkResponse.getCouchbaseMetrics().getLoadingP75());
            result.setCouchbaseLoadingP90(benchmarkResponse.getCouchbaseMetrics().getLoadingP90());
            result.setCouchbaseLoadingP99(benchmarkResponse.getCouchbaseMetrics().getLoadingP99());
            result.setCouchbaseLoadingOperationsPerSec(benchmarkResponse.getCouchbaseMetrics().getLoadingOperationsPerSec());
        }

        // Store workload type if available
        if (benchmarkResponse.getWorkloadType() != null) {
            result.setWorkloadType(benchmarkResponse.getWorkloadType().toString());
        }
        result.setThreadCount(benchmarkResponse.getThreadCount());
        saveResult(result);
        return result;
    }

    @Transactional
    public void saveResult(BenchmarkResult result) {
        if (result != null) {
            benchmarkRepository.persist(result);
        }
    }

    public List<BenchmarkHistory> getBenchmarkHistory() {
        List<BenchmarkResult> results = benchmarkRepository.listAll();
        return results.stream()
                .map(this::convertToHistory)
                .collect(Collectors.toList());
    }

    public BenchmarkHistory getBenchmarkHistoryById(Long id) {
        BenchmarkResult result = benchmarkRepository.findById(id);
        if (result == null) {
            return null;
        }
        return convertToHistory(result);
    }

    private BenchmarkHistory convertToHistory(BenchmarkResult result) {
        BenchmarkHistory history = new BenchmarkHistory();
        history.setId(result.getId());
        history.setTimestamp(result.getTimestamp());
        history.setRecordCount(result.getRecordCount());
        history.setOperationCount(result.getOperationCount());

        // Set workload type
        if (result.getWorkloadType() != null) {
            try {
                history.setWorkloadType(WorkloadType.valueOf(result.getWorkloadType()));
            } catch (IllegalArgumentException e) {
                // Default to workload A if there's a mismatch
                history.setWorkloadType(WorkloadType.A);
            }
        } else {
            history.setWorkloadType(WorkloadType.A);
        }

        // Create MongoDB metrics if available
        if (result.getMongoExecutionTime() != null) {
            DatabaseMetrics mongoMetrics = new DatabaseMetrics();

            mongoMetrics.setExecutionTime(result.getMongoExecutionTime());
            mongoMetrics.setExecutionP50(result.getMongoExecutionP50());
            mongoMetrics.setExecutionP75(result.getMongoExecutionP75());
            mongoMetrics.setExecutionP90(result.getMongoExecutionP90());
            mongoMetrics.setExecutionP99(result.getMongoExecutionP99());
            mongoMetrics.setExecuteOperationsPerSec(result.getMongoExecuteOperationsPerSec());
            mongoMetrics.setMemoryUsage(result.getMongoMemoryUsage());
            mongoMetrics.setIndexMemoryUsage(result.getMongoIndexMemoryUsage());

            mongoMetrics.setLoadingTime(result.getMongoLoadingTime());
            mongoMetrics.setLoadingP50(result.getMongoLoadingP50());
            mongoMetrics.setLoadingP75(result.getMongoLoadingP75());
            mongoMetrics.setLoadingP90(result.getMongoLoadingP90());
            mongoMetrics.setLoadingP99(result.getMongoLoadingP99());
            mongoMetrics.setLoadingOperationsPerSec(result.getMongoLoadingOperationsPerSec());

            history.setMongoMetrics(mongoMetrics);
        }

        // Create Couchbase metrics if available
        if (result.getCouchbaseExecutionTime() != null) {
            DatabaseMetrics couchbaseMetrics = new DatabaseMetrics();

            couchbaseMetrics.setExecutionTime(result.getCouchbaseExecutionTime());
            couchbaseMetrics.setExecutionP50(result.getCouchbaseExecutionP50());
            couchbaseMetrics.setExecutionP75(result.getCouchbaseExecutionP75());
            couchbaseMetrics.setExecutionP90(result.getCouchbaseExecutionP90());
            couchbaseMetrics.setExecutionP99(result.getCouchbaseExecutionP99());
            couchbaseMetrics.setExecuteOperationsPerSec(result.getCouchbaseExecuteOperationsPerSec());
            couchbaseMetrics.setMemoryUsage(result.getCouchbaseMemoryUsage());
            couchbaseMetrics.setIndexMemoryUsage(result.getCouchbaseIndexMemoryUsage());

            couchbaseMetrics.setLoadingTime(result.getCouchbaseLoadingTime());
            couchbaseMetrics.setLoadingP50(result.getCouchbaseLoadingP50());
            couchbaseMetrics.setLoadingP75(result.getCouchbaseLoadingP75());
            couchbaseMetrics.setLoadingP90(result.getCouchbaseLoadingP90());
            couchbaseMetrics.setLoadingP99(result.getCouchbaseLoadingP99());
            couchbaseMetrics.setLoadingOperationsPerSec(result.getCouchbaseLoadingOperationsPerSec());

            history.setCouchbaseMetrics(couchbaseMetrics);
        }

        return history;
    }
}