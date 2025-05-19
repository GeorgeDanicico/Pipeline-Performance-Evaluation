package com.benchmark;

import com.benchmark.benchmark.CouchbaseReadBenchmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MongoDbBenchmarkApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongoDbBenchmarkApplication.class, args);
    }
} 