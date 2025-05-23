package com.ubb.master.repository;

import com.ubb.master.model.BenchmarkResult;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BenchmarkRepository implements PanacheRepository<BenchmarkResult> {
}
