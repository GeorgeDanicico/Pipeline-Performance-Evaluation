package com.ubb.master.service;

import com.ubb.master.repository.BenchmarkRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BenchmarkService {
    private final BenchmarkRepository benchmarkRepository;

    public BenchmarkService(BenchmarkRepository benchmarkRepository) {
        this.benchmarkRepository = benchmarkRepository;
    }

    public void startBenchmark() {

    }
}
