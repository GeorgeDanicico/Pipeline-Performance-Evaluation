package com.example.ycsb;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jvm.*;
import com.example.ycsb.DB;
import com.example.ycsb.db.MongoDBClient;
import com.example.ycsb.Status;
import com.example.ycsb.ByteIterator;
import com.example.ycsb.StringByteIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MongoDBBenchmark {
    private static final String HOST = "127.0.0.1";
    private static final String DATABASE = "ycsb";
    private static final String COLLECTION = "usertable";
    private static final int RECORD_COUNT = 10000;
    private static final int OPERATION_COUNT = 10000;
    private static final int THREAD_COUNT = 1;

    // Operation proportions
    private static final double READ_PROPORTION = 0.95;
    private static final double UPDATE_PROPORTION = 0.05;
    private static final double INSERT_PROPORTION = 0;
    private static final double SCAN_PROPORTION = 0;

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

            // MongoDB specific properties
            props.setProperty("mongodb.host", HOST);
            props.setProperty("mongodb.database", DATABASE);
            props.setProperty("mongodb.collection", COLLECTION);
            props.setProperty("mongodb.timeout", "5000");

            // Initialize components
            Workload workload = new CoreWorkload();
            workload.init(props);

            DB db = new MongoDBClient();
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
        for (int i = 0; i < operationCount; i++) {
            String key = workload.nextTransactionKey();
            if (key == null) {
                break;
            }

            double random = Math.random();
            double cumulative = 0.0;

            if (random < (cumulative += READ_PROPORTION)) {
                // Read operation
                Timer.Context context = readTimer.time();
                try {
                    Status status = db.read("usertable", key, null, new HashMap<>());
                    if (status == Status.OK) {
                        readCounter.inc();
                    } else {
                        errorCounter.inc();
                    }
                } finally {
                    long time = context.stop();
                    readTotalTime += time;
                }
            } else if (random < (cumulative += UPDATE_PROPORTION)) {
                // Update operation
                HashMap<String, ByteIterator> values = new HashMap<>();
                workload.updateInit(key, values);
                Timer.Context context = updateTimer.time();
                try {
                    Status status = db.update("usertable", key, values);
                    if (status == Status.OK) {
                        updateCounter.inc();
                    } else {
                        errorCounter.inc();
                    }
                } finally {
                    long time = context.stop();
                    updateTotalTime += time;
                }
            } else if (random < (cumulative += INSERT_PROPORTION)) {
                // Insert operation
                String newKey = "user" + (RECORD_COUNT + i);
                HashMap<String, ByteIterator> values = new HashMap<>();
                workload.insertInit(newKey, values);
                Timer.Context context = insertTimer.time();
                try {
                    Status status = db.insert("usertable", newKey, values);
                    if (status == Status.OK) {
                        insertCounter.inc();
                    } else {
                        errorCounter.inc();
                    }
                } finally {
                    long time = context.stop();
                    insertTotalTime += time;
                }
            } else if (random < (cumulative += SCAN_PROPORTION)) {
                // Scan operation
                Timer.Context context = scanTimer.time();
                try {
                    Status status = db.scan("usertable", key, 10, null, new Vector<>());
                    if (status == Status.OK) {
                        scanCounter.inc();
                    } else {
                        errorCounter.inc();
                    }
                } finally {
                    long time = context.stop();
                    scanTotalTime += time;
                }
            }
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
        System.out.println("--------------------");
        System.out.printf("Read Operations:   %d (%.1f%%)\n", readCounter.getCount(), (readCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Update Operations: %d (%.1f%%)\n", updateCounter.getCount(), (updateCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Insert Operations: %d (%.1f%%)\n", insertCounter.getCount(), (insertCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Scan Operations:   %d (%.1f%%)\n", scanCounter.getCount(), (scanCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Error Count:       %d\n", errorCounter.getCount());
    }

    private static Map<String, Object> getTimerStats(Timer timer) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", timer.getCount());
        stats.put("mean", timer.getSnapshot().getMean());
        stats.put("p95", timer.getSnapshot().get95thPercentile());
        stats.put("p99", timer.getSnapshot().get99thPercentile());
        stats.put("max", timer.getSnapshot().getMax());
        return stats;
    }

    private static Map<String, Object> getJvmMetrics() {
        Map<String, Object> jvmMetrics = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        jvmMetrics.put("memory.total", runtime.totalMemory());
        jvmMetrics.put("memory.free", runtime.freeMemory());
        jvmMetrics.put("memory.max", runtime.maxMemory());
        jvmMetrics.put("threads.active", Thread.activeCount());
        return jvmMetrics;
    }
} 