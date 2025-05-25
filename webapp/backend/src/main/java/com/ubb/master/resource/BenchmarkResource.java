package com.ubb.master.resource;

import com.ubb.master.generated.api.BenchmarkApi;
import com.ubb.master.generated.api.model.BenchmarkRequest;
import com.ubb.master.generated.api.model.BenchmarkResponse;
import com.ubb.master.service.BenchmarkService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class BenchmarkResource implements BenchmarkApi {

    private final BenchmarkService benchmarkService;

    public BenchmarkResource(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @Override
    public BenchmarkResponse startBenchmark(BenchmarkRequest benchmarkRequest) {
        log.info("Starting benchmark with request: {}", benchmarkRequest);
        benchmarkService.startBenchmark();

        BenchmarkResponse response = new BenchmarkResponse();
        response.setMessage("Benchmark has started successfully");
        return response;
    }
}