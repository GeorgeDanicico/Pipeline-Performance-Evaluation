package com.benchmark.benchmark;

import com.benchmark.model.TaxiDocument;
import com.mongodb.ExplainVerbosity;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static com.benchmark.generator.RandomMongoQueryGenerator.buildRandomFilter;

public class MongoReadBenchmark {
    private ConfigurableApplicationContext context;
    private MongoCollection<Document> collection;

    public void setup() {
        context = SpringApplication.run(com.benchmark.MongoDbBenchmarkApplication.class);
        MongoClient mongoClient = context.getBean(MongoClient.class);
        MongoDatabase database = mongoClient.getDatabase("census");
        collection = database.getCollection("nyc_taxi");
    }

    public void tearDown() {
        context.close();
    }

    public List<TaxiDocument> findAll() {
        List<TaxiDocument> results = new ArrayList<>();
        collection.find().forEach(doc -> results.add(TaxiDocument.fromDocument(doc)));
        return results;
    }

    public Object simpleMatchAggregation() {

        // Execute the aggregation and collect results
        var executionStats = collection.find(buildRandomFilter()).explain(ExplainVerbosity.EXECUTION_STATS);

        return executionStats;
    }

    public List<TaxiDocument> findAllWithLimit() {
        List<TaxiDocument> results = new ArrayList<>();
        collection.find().limit(1000).forEach(doc -> results.add(TaxiDocument.fromDocument(doc)));
        return results;
    }

    public List<TaxiDocument> findAllWithProjection() {
        List<TaxiDocument> results = new ArrayList<>();
        Document projection = new Document()
            .append("VendorID", 1)
            .append("trip_distance", 1)
            .append("fare_amount", 1)
            .append("_id", 0);
        collection.find().projection(projection).forEach(doc -> {
            TaxiDocument taxiDoc = new TaxiDocument();
            taxiDoc.setVendorId(doc.getInteger("VendorID"));
            taxiDoc.setTripDistance(doc.getDouble("trip_distance"));
            taxiDoc.setFareAmount(doc.getDouble("fare_amount"));
            results.add(taxiDoc);
        });
        return results;
    }

    public static void main(String[] args) {
        MongoReadBenchmark benchmark = new MongoReadBenchmark();
        benchmark.setup();
        
        try {
            // Example of how to run the benchmarks manually
            System.out.println("Starting benchmarks...");
            
            // Run findAll
            long startTime = System.nanoTime();
            var result = benchmark.simpleMatchAggregation();
            long endTime = System.nanoTime();
            System.out.println("findAll took " + (endTime - startTime) / 1_000_000.0 + " ms");
//            System.out.println("Found " + results.size() + " documents");
            
//            // Run findAllWithLimit
//            startTime = System.nanoTime();
//            results = benchmark.findAllWithLimit();
//            endTime = System.nanoTime();
//            System.out.println("findAllWithLimit took " + (endTime - startTime) / 1_000_000.0 + " ms");
//            System.out.println("Found " + results.size() + " documents");
//
//            // Run findAllWithProjection
//            startTime = System.nanoTime();
//            results = benchmark.findAllWithProjection();
//            endTime = System.nanoTime();
//            System.out.println("findAllWithProjection took " + (endTime - startTime) / 1_000_000.0 + " ms");
//            System.out.println("Found " + results.size() + " documents");
            
        } finally {
            benchmark.tearDown();
        }
    }
} 