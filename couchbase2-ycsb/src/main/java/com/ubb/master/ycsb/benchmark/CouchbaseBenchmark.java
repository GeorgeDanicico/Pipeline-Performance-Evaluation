package com.ubb.master.ycsb.benchmark;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jvm.*;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubb.master.ycsb.db.CouchbaseClient;
import com.ubb.master.ycsb.db.DB;
import com.ubb.master.ycsb.enums.Status;
import com.ubb.master.ycsb.iterator.ByteIterator;
import com.ubb.master.ycsb.workload.CoreWorkload;
import com.ubb.master.ycsb.workload.Workload;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class CouchbaseBenchmark {
    private static final String HOST = "127.0.0.1";
    private static final String BUCKET_NAME = "census";
    private static final String BUCKET_PASSWORD = "parola03";
    private static final int RECORD_COUNT = 10000;
    private static final int OPERATION_COUNT = 10000;
    private static final int THREAD_COUNT = 1;

    // Operation proportions
    private static final double READ_PROPORTION = 0.5;
    private static final double UPDATE_PROPORTION = 0.5;
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

    // Add concurrent lists for storing latencies
    private static final CopyOnWriteArrayList<Long> readLatencies = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Long> updateLatencies = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Long> insertLatencies = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Long> scanLatencies = new CopyOnWriteArrayList<>();

    // Add phase timing variables
    private static final AtomicLong loadPhaseStartTime = new AtomicLong(0);
    private static final AtomicLong loadPhaseEndTime = new AtomicLong(0);
    private static final AtomicLong runPhaseStartTime = new AtomicLong(0);
    private static final AtomicLong runPhaseEndTime = new AtomicLong(0);

    public static void main(String[] args) {
        try {
            // Register JVM metrics
            metrics.register("jvm.gc", new GarbageCollectorMetricSet());
            metrics.register("jvm.memory", new MemoryUsageGaugeSet());
            metrics.register("jvm.threads", new ThreadStatesGaugeSet());
            metrics.register("jvm.files", new FileDescriptorRatioGauge());

            // Initialize Couchbase connection
            ClusterEnvironment env = ClusterEnvironment.builder()
                    .timeoutConfig(timeoutConfig -> timeoutConfig
                            .connectTimeout(Duration.ofMillis(30000))
                            .kvTimeout(Duration.ofMillis(10000)))
                    .build();

            // Create cluster options
            ClusterOptions options = ClusterOptions.clusterOptions(BUCKET_NAME, BUCKET_PASSWORD)
                    .environment(env);

            // Connect to cluster
            Cluster cluster = Cluster.connect(HOST, options);
            Bucket bucket = cluster.bucket(BUCKET_NAME);

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
            props.setProperty("couchbase.username", "census");
            props.setProperty("couchbase.password", BUCKET_PASSWORD);
            props.setProperty("couchbase.connectTimeout", "30000");
            props.setProperty("couchbase.kvTimeout", "10000");
            props.setProperty("couchbase.queryTimeout", "75000");
            props.setProperty("couchbase.maxParallelism", String.valueOf(THREAD_COUNT));
            props.setProperty("couchbase.kvEndpoints", String.valueOf(THREAD_COUNT));
            props.setProperty("couchbase.queryEndpoints", String.valueOf(THREAD_COUNT * 2));

            // Initialize components
            Workload workload = new CoreWorkload();
            workload.init(props);

            DB db = new CouchbaseClient();
            db.setProperties(props);
            db.init();

            // Load phase
            System.out.println("Starting load phase...");
            loadData(db, workload, RECORD_COUNT);
            System.out.println("Load phase completed.");

            // Run phase
//            System.out.println("Starting run phase...");
//            benchmarkStartTime = System.nanoTime();
//            runBenchmark(db, workload, OPERATION_COUNT, THREAD_COUNT);
//            System.out.println("Run phase completed.");

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
        loadPhaseStartTime.set(System.nanoTime());
        
        List<Thread> threads = new ArrayList<>();
        int recordsPerThread = recordCount / THREAD_COUNT;
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    int startRecord = threadId * recordsPerThread;
                    int endRecord = (threadId == THREAD_COUNT - 1) ? recordCount : (threadId + 1) * recordsPerThread;
                    
                    for (int j = startRecord; j < endRecord; j++) {
                        String key = String.format("user%d", j);
                        HashMap<String, ByteIterator> values = new HashMap<>();
                        workload.insertInit(key, values);
                        long startTime = System.nanoTime();
                        Status status = db.insert("usertable", key, values);
                        long endTime = System.nanoTime();
                        insertLatencies.add(endTime - startTime);
                        
                        if (status != Status.OK) {
                            System.err.println("Error inserting record: " + key);
                            errorCounter.inc();
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
        
        loadPhaseEndTime.set(System.nanoTime());
    }

    private static void runBenchmark(DB db, Workload workload, int operationCount, int threadCount) throws Exception {
        runPhaseStartTime.set(System.nanoTime());
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < operationCount / threadCount; j++) {
                        String key = workload.nextTransactionKey();
                        if (key == null) {
                            break;
                        }

                        double random = Math.random();
                        if (random < READ_PROPORTION) {
                            // Perform read operation
                            HashMap<String, ByteIterator> result = new HashMap<>();
                            long startTime = System.nanoTime();
                            Status status = db.read("usertable", key, null, result);
                            long endTime = System.nanoTime();
                            readLatencies.add(endTime - startTime);
                            readCounter.inc();
                            if (status != Status.OK) {
                                errorCounter.inc();
                            }
                        } else if (random < READ_PROPORTION + UPDATE_PROPORTION) {
                            // Perform update operation
                            HashMap<String, ByteIterator> values = new HashMap<>();
                            workload.updateInit(key, values);
                            long startTime = System.nanoTime();
                            Status status = db.update("usertable", key, values);
                            long endTime = System.nanoTime();
                            updateLatencies.add(endTime - startTime);
                            updateCounter.inc();
                            if (status != Status.OK) {
                                errorCounter.inc();
                            }
                        } else if (random < READ_PROPORTION + UPDATE_PROPORTION + INSERT_PROPORTION) {
                            // Perform insert operation
                            HashMap<String, ByteIterator> values = new HashMap<>();
                            workload.insertInit(key, values);
                            long startTime = System.nanoTime();
                            Status status = db.insert("usertable", key, values);
                            long endTime = System.nanoTime();
                            insertLatencies.add(endTime - startTime);
                            insertCounter.inc();
                            if (status != Status.OK) {
                                errorCounter.inc();
                            }
                        } else {
                            // Perform scan operation
                            Vector<HashMap<String, ByteIterator>> result = new Vector<>();
                            long startTime = System.nanoTime();
                            Status status = db.scan("usertable", key, 10, null, result);
                            long endTime = System.nanoTime();
                            scanLatencies.add(endTime - startTime);
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
        runPhaseEndTime.set(System.nanoTime());
    }

    private static void saveMetrics() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> metricsMap = new HashMap<>();
        
        // Add phase timing metrics
        double loadPhaseTime = (loadPhaseEndTime.get() - loadPhaseStartTime.get()) / 1_000_000.0;
        double runPhaseTime = (runPhaseEndTime.get() - runPhaseStartTime.get()) / 1_000_000.0;
        metricsMap.put("load_phase_time_ms", loadPhaseTime);
        metricsMap.put("run_phase_time_ms", runPhaseTime);
        
        // Add latency metrics for each operation type
        Map<String, Double> readMetrics = calculatePercentiles(readLatencies);
        Map<String, Double> updateMetrics = calculatePercentiles(updateLatencies);
        Map<String, Double> insertMetrics = calculatePercentiles(insertLatencies);
        Map<String, Double> scanMetrics = calculatePercentiles(scanLatencies);
        
        metricsMap.put("read_latencies", readMetrics);
        metricsMap.put("update_latencies", updateMetrics);
        metricsMap.put("insert_latencies", insertMetrics);
        metricsMap.put("scan_latencies", scanMetrics);
        
        // Save to file
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File("metrics.json"), metricsMap);
        
        // Print detailed console output
        System.out.println("\n=== Benchmark Results ===");
        
        // Print phase times
        System.out.println("\nPhase Times:");
        System.out.println("--------------------");
        System.out.printf("Load Phase Time: %.2f ms\n", loadPhaseTime);
        System.out.printf("Run Phase Time:  %.2f ms\n", runPhaseTime);
        
        // Print throughput
        System.out.println("\nThroughput:");
        System.out.println("--------------------");
        System.out.printf("Load Phase: %.2f ops/sec\n", (RECORD_COUNT / (loadPhaseTime / 1000.0)));
        System.out.printf("Run Phase:  %.2f ops/sec\n", (OPERATION_COUNT / (runPhaseTime / 1000.0)));
        
        // Print operation latencies
        System.out.println("\nOperation Latencies (milliseconds):");
        System.out.println("--------------------------------");
        
        // Print read metrics
        if (!readLatencies.isEmpty()) {
            System.out.println("\nRead Operations:");
            System.out.println("--------------------");
            System.out.printf("Count:     %d\n", readLatencies.size());
            System.out.printf("Min:       %.2f ms\n", readMetrics.get("min"));
            System.out.printf("Mean:      %.2f ms\n", readMetrics.get("mean"));
            System.out.printf("P50:       %.2f ms\n", readMetrics.get("p50"));
            System.out.printf("P75:       %.2f ms\n", readMetrics.get("p75"));
            System.out.printf("P90:       %.2f ms\n", readMetrics.get("p90"));
            System.out.printf("P99:       %.2f ms\n", readMetrics.get("p99"));
            System.out.printf("Max:       %.2f ms\n", readMetrics.get("max"));
        }
        
        // Print update metrics
        if (!updateLatencies.isEmpty()) {
            System.out.println("\nUpdate Operations:");
            System.out.println("--------------------");
            System.out.printf("Count:     %d\n", updateLatencies.size());
            System.out.printf("Min:       %.2f ms\n", updateMetrics.get("min"));
            System.out.printf("Mean:      %.2f ms\n", updateMetrics.get("mean"));
            System.out.printf("P50:       %.2f ms\n", updateMetrics.get("p50"));
            System.out.printf("P75:       %.2f ms\n", updateMetrics.get("p75"));
            System.out.printf("P90:       %.2f ms\n", updateMetrics.get("p90"));
            System.out.printf("P99:       %.2f ms\n", updateMetrics.get("p99"));
            System.out.printf("Max:       %.2f ms\n", updateMetrics.get("max"));
        }
        
        // Print insert metrics
        if (!insertLatencies.isEmpty()) {
            System.out.println("\nInsert Operations:");
            System.out.println("--------------------");
            System.out.printf("Count:     %d\n", insertLatencies.size());
            System.out.printf("Min:       %.2f ms\n", insertMetrics.get("min"));
            System.out.printf("Mean:      %.2f ms\n", insertMetrics.get("mean"));
            System.out.printf("P50:       %.2f ms\n", insertMetrics.get("p50"));
            System.out.printf("P75:       %.2f ms\n", insertMetrics.get("p75"));
            System.out.printf("P90:       %.2f ms\n", insertMetrics.get("p90"));
            System.out.printf("P99:       %.2f ms\n", insertMetrics.get("p99"));
            System.out.printf("Max:       %.2f ms\n", insertMetrics.get("max"));
        }
        
        // Print scan metrics
        if (!scanLatencies.isEmpty()) {
            System.out.println("\nScan Operations:");
            System.out.println("--------------------");
            System.out.printf("Count:     %d\n", scanLatencies.size());
            System.out.printf("Min:       %.2f ms\n", scanMetrics.get("min"));
            System.out.printf("Mean:      %.2f ms\n", scanMetrics.get("mean"));
            System.out.printf("P50:       %.2f ms\n", scanMetrics.get("p50"));
            System.out.printf("P75:       %.2f ms\n", scanMetrics.get("p75"));
            System.out.printf("P90:       %.2f ms\n", scanMetrics.get("p90"));
            System.out.printf("P99:       %.2f ms\n", scanMetrics.get("p99"));
            System.out.printf("Max:       %.2f ms\n", scanMetrics.get("max"));
        }
        
        // Print operation distribution
        System.out.println("\nOperation Distribution:");
        System.out.println("--------------------");
        System.out.printf("Read Operations:   %d (%.1f%%)\n", readCounter.getCount(), (readCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Update Operations: %d (%.1f%%)\n", updateCounter.getCount(), (updateCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Insert Operations: %d (%.1f%%)\n", insertCounter.getCount(), (insertCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Scan Operations:   %d (%.1f%%)\n", scanCounter.getCount(), (scanCounter.getCount() * 100.0) / OPERATION_COUNT);
        System.out.printf("Total Errors:      %d\n", errorCounter.getCount());
        
        System.out.println("\nDetailed metrics have been saved to metrics.json");
    }

    private static Map<String, Double> calculatePercentiles(List<Long> latencies) {
        Map<String, Double> percentiles = new HashMap<>();
        if (latencies.isEmpty()) {
            return percentiles;
        }

        // Convert to array and sort
        Long[] sortedLatencies = latencies.toArray(new Long[0]);
        Arrays.sort(sortedLatencies);

        // Calculate percentiles (convert from nanoseconds to milliseconds)
        percentiles.put("p50", sortedLatencies[(int) (sortedLatencies.length * 0.50)] / 1_000_000.0);
        percentiles.put("p75", sortedLatencies[(int) (sortedLatencies.length * 0.75)] / 1_000_000.0);
        percentiles.put("p90", sortedLatencies[(int) (sortedLatencies.length * 0.90)] / 1_000_000.0);
        percentiles.put("p99", sortedLatencies[(int) (sortedLatencies.length * 0.99)] / 1_000_000.0);
        percentiles.put("min", sortedLatencies[0] / 1_000_000.0);
        percentiles.put("max", sortedLatencies[sortedLatencies.length - 1] / 1_000_000.0);
        percentiles.put("mean", latencies.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0);

        return percentiles;
    }

    private static void printLatencyStats(String operation, List<Long> latencies) {
        if (latencies.isEmpty()) {
            System.out.printf("%s: No operations performed\n", operation);
            return;
        }

        Map<String, Double> percentiles = calculatePercentiles(latencies);
        System.out.printf("\n%s Operations:\n", operation);
        System.out.printf("  Count:    %d\n", latencies.size());
        System.out.printf("  Min:      %.2f ms\n", percentiles.get("min"));
        System.out.printf("  Mean:     %.2f ms\n", percentiles.get("mean"));
        System.out.printf("  P50:      %.2f ms\n", percentiles.get("p50"));
        System.out.printf("  P75:      %.2f ms\n", percentiles.get("p75"));
        System.out.printf("  P90:      %.2f ms\n", percentiles.get("p90"));
        System.out.printf("  P99:      %.2f ms\n", percentiles.get("p99"));
        System.out.printf("  Max:      %.2f ms\n", percentiles.get("max"));
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