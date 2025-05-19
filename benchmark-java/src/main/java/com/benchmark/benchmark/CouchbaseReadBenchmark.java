package com.benchmark.benchmark;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryMetrics;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.transactions.TransactionQueryOptions;
import com.couchbase.client.java.transactions.config.TransactionOptions;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.benchmark.generator.RandomCouchbaseQueryGenerator.randomQuery;
import static com.couchbase.client.java.query.QueryOptions.queryOptions;

@Component
public class CouchbaseReadBenchmark {

    private ConfigurableApplicationContext context;
    private Cluster cluster;
    private Collection collection;

    public void setup() {
        context = SpringApplication.run(com.benchmark.MongoDbBenchmarkApplication.class);
        cluster = context.getBean(Cluster.class);
        collection = context.getBean(Collection.class);
    }

    public void tearDown() {
        context.close();
    }

    public void printQueryPlan(String query) {
        String explainQuery = "EXPLAIN " + query;
        QueryResult result = cluster.query(explainQuery);
        System.out.println("\nQuery Execution Plan:");
        System.out.println(result.rowsAsObject().get(0).toString());
    }

    public List<Long> runReadBenchmark(int iterations) {
        List<Long> executionTimes = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            // Perform read operation using N1QL query
            String query = randomQuery();
            
            // Print query plan before execution
            printQueryPlan(query);

//            cluster.transactions().run(ctx -> {
//                // Run a N1QL query transactionally
//                String statement = "SELECT * FROM `census` WHERE VendorID = $vid";
//                var qr = ctx.query(query, TransactionQueryOptions.queryOptions()
//                                .adhoc(false)
//                                .scanWait(Duration.ofSeconds(60))
//                    .pipelineBatch(50)
//                    .scanCap(1024)
//                    .readonly(true)
//                ).metaData().metrics().get();  // N1QL inside transaction
//
//                System.out.println(qr);
//            }, TransactionOptions.transactionOptions()
//                            .timeout(Duration.ofSeconds(60)));

            cluster.query(query, queryOptions()
                    .timeout(Duration.ofSeconds(60))
                    .pipelineBatch(50)
                    .scanCap(1024)
                    .readonly(true));
            
            // Get query metrics
//            QueryMetrics metrics = result.metaData().metrics().get();
            
            // Print detailed metrics
//            System.out.println("\nQuery Execution Stats:");
//            System.out.println("Execution Time: " + metrics.executionTime() + " ms");
//            System.out.println("Elapsed Time: " + metrics.elapsedTime() + " ms");
//            System.out.println("Result Count: " + metrics.resultCount());
//            System.out.println("Result Size: " + metrics.resultSize() + " bytes");
//            System.out.println("Sort Count: " + metrics.sortCount());
//            System.out.println("Mutation Count: " + metrics.mutationCount());
//            System.out.println("Error Count: " + metrics.errorCount());
//            System.out.println("Warning Count: " + metrics.warningCount());
            
            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            executionTimes.add(duration);
            
            // Optional: Add a small delay between iterations
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return executionTimes;
    }

    public static void main(String[] args) {
        CouchbaseReadBenchmark benchmark = new CouchbaseReadBenchmark();
        benchmark.setup();

        try {
            // Example of how to run the benchmarks manually
            System.out.println("Starting benchmarks...");

            // Run findAll
            long startTime = System.nanoTime();
            benchmark.runReadBenchmark(1);
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