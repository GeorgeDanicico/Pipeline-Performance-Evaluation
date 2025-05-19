package com.example.ycsb;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jvm.*;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Couchbase2Benchmark {
    private static final String HOST = "127.0.0.1";
    private static final String BUCKET_NAME = "census";
    private static final String BUCKET_PASSWORD = "parola03";
    private static final int RECORD_COUNT = 10000;
    private static final int OPERATION_COUNT = 1000;
    private static final int THREAD_COUNT = 1;

    // Operation proportions
    private static final double READ_PROPORTION = 0.5;
    private static final double UPDATE_PROPORTION = 0.3;
    private static final double INSERT_PROPORTION = 0.1;
    private static final double SCAN_PROPORTION = 0.1;

    private static final MetricRegistry metrics = new MetricRegistry();
    private static final Timer readTimer = metrics.timer("read");
    private static final Timer updateTimer = metrics.timer("update");
    private static final Timer insertTimer = metrics.timer("insert");
    private static final Timer scanTimer = metrics.timer("scan");
    private static final Counter readCounter = metrics.counter("read.count");
    private static final Counter updateCounter = metrics.counter("update.count");
    private static final Counter insertCounter = metrics.counter("insert.count");
    private static final Counter scanCounter = metrics.counter("scan.count");
    private static final Counter errorCounter = metrics.counter("error.count");

    // Add timers for total execution time
    private static long readTotalTime = 0;
    private static long updateTotalTime = 0;
    private static long insertTotalTime = 0;
    private static long scanTotalTime = 0;
    private static long benchmarkStartTime = 0;

    public static void main(String[] args) {
        try {
            // Register JVM metrics
            metrics.register("jvm.gc", new GarbageCollectorMetricSet());
            metrics.register("jvm.memory", new MemoryUsageGaugeSet());
            metrics.register("jvm.threads", new ThreadStatesGaugeSet());
            metrics.register("jvm.files", new FileDescriptorRatioGauge());

            // Initialize Couchbase connection
            CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
                    .connectTimeout(30000)
                    .kvTimeout(10000)
                    .build();

            Cluster cluster = CouchbaseCluster.create(env, HOST);
            Bucket bucket = cluster.openBucket(BUCKET_NAME, BUCKET_PASSWORD);

            // Create workload
            Properties props = new Properties();
            props.setProperty("recordcount", String.valueOf(RECORD_COUNT));
            props.setProperty("operationcount", String.valueOf(OPERATION_COUNT));
            props.setProperty("fieldcount", "10");
            props.setProperty("fieldlength", "100");
            props.setProperty("readproportion", String.valueOf(READ_PROPORTION));
            props.setProperty("updateproportion", String.valueOf(UPDATE_PROPORTION));
            props.setProperty("scanproportion", String.valueOf(SCAN_PROPORTION));
            props.setProperty("insertproportion", String.valueOf(INSERT_PROPORTION));
            props.setProperty("requestdistribution", "zipfian");

            // Couchbase specific properties
            props.setProperty("couchbase.host", HOST);
            props.setProperty("couchbase.bucket", BUCKET_NAME);
            props.setProperty("couchbase.password", BUCKET_PASSWORD);
            props.setProperty("couchbase.syncMutationResponse", "true");
            props.setProperty("couchbase.persistTo", "0");
            props.setProperty("couchbase.replicateTo", "0");
            props.setProperty("couchbase.upsert", "false");
            props.setProperty("couchbase.adhoc", "false");
            props.setProperty("couchbase.kv", "true");
            props.setProperty("couchbase.maxParallelism", "1");
            props.setProperty("couchbase.kvEndpoints", "1");
            props.setProperty("couchbase.queryEndpoints", "5");
            props.setProperty("couchbase.epoll", "false");
            props.setProperty("couchbase.boost", "3");
            props.setProperty("couchbase.networkMetricsInterval", "0");
            props.setProperty("couchbase.runtimeMetricsInterval", "0");
            props.setProperty("couchbase.documentExpiry", "0");

            // Initialize components
            Workload workload = new CoreWorkload();
            workload.init(props);

            DB db = new Couchbase2Client();
            db.setProperties(props);
            db.init();

            // Load phase
//            System.out.println("Starting load phase...");
//            loadData(db, workload, RECORD_COUNT);
//            System.out.println("Load phase completed.");

            // Run phase
            System.out.println("Starting run phase...");
            benchmarkStartTime = System.nanoTime();
            runBenchmark(db, workload, OPERATION_COUNT, THREAD_COUNT);
            System.out.println("Run phase completed.");

            // Save metrics
            saveMetrics();

            // Cleanup
            db.cleanup();
            cluster.disconnect();
            workload.cleanup();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadData(DB db, Workload workload, int recordCount) throws Exception {
        for (int i = 0; i < recordCount; i++) {
            String key = String.format("user%d", i);
            HashMap<String, ByteIterator> values = new HashMap<>();
            workload.insertInit(key, values);
            Status status = db.insert("usertable", key, values);
            if (status != Status.OK) {
                System.err.println("Error inserting record: " + key);
                errorCounter.inc();
            }
        }
    }

    private static void runBenchmark(DB db, Workload workload, int operationCount, int threadCount) throws Exception {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < operationCount / threadCount; j++) {
                        String key = workload.nextTransactionKey();
                        if (key == null) {
                            break;
                        }

                        // Determine operation type based on proportions
                        double random = Math.random();
                        if (random < READ_PROPORTION) {
                            // Perform read operation
                            HashMap<String, ByteIterator> result = new HashMap<>();
                            long startTime = System.nanoTime();
                            Timer.Context readContext = readTimer.time();
                            Status status = db.read("usertable", key, null, result);
                            readContext.stop();
                            readTotalTime += (System.nanoTime() - startTime);
                            readCounter.inc();
                            if (status != Status.OK) {
                                errorCounter.inc();
                            }
                        } else if (random < READ_PROPORTION + UPDATE_PROPORTION) {
                            // Perform update operation
                            HashMap<String, ByteIterator> values = new HashMap<>();
                            workload.updateInit(key, values);
                            long startTime = System.nanoTime();
                            Timer.Context updateContext = updateTimer.time();
                            Status status = db.update("usertable", key, values);
                            updateContext.stop();
                            updateTotalTime += (System.nanoTime() - startTime);
                            updateCounter.inc();
                            if (status != Status.OK) {
                                errorCounter.inc();
                            }
                        } else if (random < READ_PROPORTION + UPDATE_PROPORTION + INSERT_PROPORTION) {
                            // Perform insert operation
                            HashMap<String, ByteIterator> values = new HashMap<>();
                            workload.insertInit(key, values);
                            long startTime = System.nanoTime();
                            Timer.Context insertContext = insertTimer.time();
                            Status status = db.insert("usertable", key, values);
                            insertContext.stop();
                            insertTotalTime += (System.nanoTime() - startTime);
                            insertCounter.inc();
                            if (status != Status.OK) {
                                errorCounter.inc();
                            }
                        } else {
                            // Perform scan operation
                            Vector<HashMap<String, ByteIterator>> result = new Vector<>();
                            long startTime = System.nanoTime();
                            Timer.Context scanContext = scanTimer.time();
                            Status status = db.scan("usertable", key, 10, null, result);
                            scanContext.stop();
                            scanTotalTime += (System.nanoTime() - startTime);
                            scanCounter.inc();
                            if (status != Status.OK) {
                                errorCounter.inc();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorCounter.inc();
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void saveMetrics() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> metricsMap = new HashMap<>();
        
        // Add timer metrics
        metricsMap.put("read", getTimerStats(readTimer));
        metricsMap.put("update", getTimerStats(updateTimer));
        metricsMap.put("insert", getTimerStats(insertTimer));
        metricsMap.put("scan", getTimerStats(scanTimer));
        
        // Add counter metrics
        metricsMap.put("read.count", readCounter.getCount());
        metricsMap.put("update.count", updateCounter.getCount());
        metricsMap.put("insert.count", insertCounter.getCount());
        metricsMap.put("scan.count", scanCounter.getCount());
        metricsMap.put("error.count", errorCounter.getCount());
        
        // Add JVM metrics
        metricsMap.put("jvm", getJvmMetrics());
        
        // Save to file
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File("metrics.json"), metricsMap);
        
        // Calculate total benchmark time
        long totalBenchmarkTime = System.nanoTime() - benchmarkStartTime;
        
        // Print detailed console output
        System.out.println("\n=== Benchmark Results ===");
        System.out.println("\nTotal Execution Time:");
        System.out.println("--------------------");
        System.out.printf("Total Benchmark Time: %.2f seconds\n", totalBenchmarkTime / 1_000_000_000.0);
        System.out.printf("Read Operations:      %.2f seconds\n", readTotalTime / 1_000_000_000.0);
        System.out.printf("Update Operations:    %.2f seconds\n", updateTotalTime / 1_000_000_000.0);
        System.out.printf("Insert Operations:    %.2f seconds\n", insertTotalTime / 1_000_000_000.0);
        System.out.printf("Scan Operations:      %.2f seconds\n", scanTotalTime / 1_000_000_000.0);
        
        System.out.println("\nOperation Distribution:");
        System.out.println("----------------------");
        System.out.printf("Read Operations:   %d (%.1f%%)\n", readCounter.getCount(), (readCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Update Operations: %d (%.1f%%)\n", updateCounter.getCount(), (updateCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Insert Operations: %d (%.1f%%)\n", insertCounter.getCount(), (insertCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Scan Operations:   %d (%.1f%%)\n", scanCounter.getCount(), (scanCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Total Errors:      %d\n", errorCounter.getCount());
        
        System.out.println("\nRead Operation Metrics:");
        System.out.println("---------------------");
        System.out.println("Latency:");
        printTimerStats(readTimer);
        System.out.println("\nThroughput:");
        printThroughputStats(readTimer);
        
        System.out.println("\nUpdate Operation Metrics:");
        System.out.println("----------------------");
        System.out.println("Latency:");
        printTimerStats(updateTimer);
        System.out.println("\nThroughput:");
        printThroughputStats(updateTimer);

        System.out.println("\nInsert Operation Metrics:");
        System.out.println("----------------------");
        System.out.println("Latency:");
        printTimerStats(insertTimer);
        System.out.println("\nThroughput:");
        printThroughputStats(insertTimer);

        System.out.println("\nScan Operation Metrics:");
        System.out.println("---------------------");
        System.out.println("Latency:");
        printTimerStats(scanTimer);
        System.out.println("\nThroughput:");
        printThroughputStats(scanTimer);
        
        System.out.println("\nDetailed metrics have been saved to metrics.json");
    }

    private static void printTimerStats(Timer timer) {
        Snapshot snapshot = timer.getSnapshot();
        // Convert from nanoseconds to microseconds
        double convertToMicros = 1000.0; // 1 millisecond = 1000 microseconds
        
        System.out.printf("Count:     %d operations\n", timer.getCount());
        System.out.printf("Mean:      %.2f μs\n", snapshot.getMean() / convertToMicros);
        System.out.printf("Median:    %.2f μs\n", snapshot.getMedian() / convertToMicros);
        System.out.printf("75th %%:    %.2f μs\n", snapshot.get75thPercentile() / convertToMicros);
        System.out.printf("95th %%:    %.2f μs\n", snapshot.get95thPercentile() / convertToMicros);
        System.out.printf("99th %%:    %.2f μs\n", snapshot.get99thPercentile() / convertToMicros);
        System.out.printf("99.9th %%:  %.2f μs\n", snapshot.get999thPercentile() / convertToMicros);
        System.out.printf("StdDev:    %.2f μs\n", snapshot.getStdDev() / convertToMicros);
    }

    private static void printThroughputStats(Timer timer) {
        System.out.printf("Mean Rate:     %.2f ops/sec\n", timer.getMeanRate());
        System.out.printf("1-Min Rate:    %.2f ops/sec\n", timer.getOneMinuteRate());
        System.out.printf("5-Min Rate:    %.2f ops/sec\n", timer.getFiveMinuteRate());
        System.out.printf("15-Min Rate:   %.2f ops/sec\n", timer.getFifteenMinuteRate());
    }

    private static Map<String, Object> getTimerStats(Timer timer) {
        Map<String, Object> stats = new HashMap<>();
        Snapshot snapshot = timer.getSnapshot();
        
        stats.put("count", timer.getCount());
        stats.put("mean", snapshot.getMean());
        stats.put("median", snapshot.getMedian());
        stats.put("p75", snapshot.get75thPercentile());
        stats.put("p95", snapshot.get95thPercentile());
        stats.put("p99", snapshot.get99thPercentile());
        stats.put("p999", snapshot.get999thPercentile());
        stats.put("max", snapshot.getMax());
        stats.put("min", snapshot.getMin());
        stats.put("stddev", snapshot.getStdDev());
        stats.put("m15_rate", timer.getFifteenMinuteRate());
        stats.put("m5_rate", timer.getFiveMinuteRate());
        stats.put("m1_rate", timer.getOneMinuteRate());
        stats.put("mean_rate", timer.getMeanRate());
        
        return stats;
    }

    private static Map<String, Object> getJvmMetrics() {
        Map<String, Object> jvmMetrics = new HashMap<>();
        
//        // Get memory metrics
//        MemoryUsageGaugeSet memoryGauges = new MemoryUsageGaugeSet();
//        for (Map.Entry<String, Gauge> entry : memoryGauges.getGauges().entrySet()) {
//            jvmMetrics.put(entry.getKey(), entry.getValue().getValue());
//        }
//
//        // Get GC metrics
//        GarbageCollectorMetricSet gcMetrics = new GarbageCollectorMetricSet();
//        for (Map.Entry<String, Gauge> entry : gcMetrics.getGauges().entrySet()) {
//            jvmMetrics.put(entry.getKey(), entry.getValue().getValue());
//        }
        
        return jvmMetrics;
    }
}