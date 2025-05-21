package com.ubb.master.ycsb;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
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

public class BenchmarkRunner {
    public enum BenchmarkType {
        LOAD,
        RUN
    }

    public enum WorkloadType {
        READ_HEAVY(0.8, 0.2, 0.0, 0.0),
        WRITE_HEAVY(0.2, 0.8, 0.0, 0.0),
        READ_ONLY(1.0, 0.0, 0.0, 0.0),
        WRITE_ONLY(0.0, 1.0, 0.0, 0.0),
        MIXED(0.4, 0.4, 0.1, 0.1),
        CUSTOM(0.0, 0.0, 0.0, 0.0);

        private final double readProportion;
        private final double updateProportion;
        private final double insertProportion;
        private final double scanProportion;

        WorkloadType(double read, double update, double insert, double scan) {
            this.readProportion = read;
            this.updateProportion = update;
            this.insertProportion = insert;
            this.scanProportion = scan;
        }

        public double getReadProportion() { return readProportion; }
        public double getUpdateProportion() { return updateProportion; }
        public double getInsertProportion() { return insertProportion; }
        public double getScanProportion() { return scanProportion; }
    }

    private static final String HOST = "127.0.0.1";
    private static final String BUCKET_NAME = "census";
    private static final String BUCKET_PASSWORD = "parola03";
    private static final int RECORD_COUNT = 10000;
    private static final int OPERATION_COUNT = 10000;
    private static final int THREAD_COUNT = 1;

    private final MetricRegistry metrics = new MetricRegistry();
    private final Timer readTimer = metrics.timer("read");
    private final Timer updateTimer = metrics.timer("update");
    private final Timer insertTimer = metrics.timer("insert");
    private final Timer scanTimer = metrics.timer("scan");
    private final Counter readCounter = metrics.counter("read.count");
    private final Counter updateCounter = metrics.counter("update.count");
    private final Counter insertCounter = metrics.counter("insert.count");
    private final Counter scanCounter = metrics.counter("scan.count");
    private final Counter errorCounter = metrics.counter("error.count");

    private long readTotalTime = 0;
    private long updateTotalTime = 0;
    private long insertTotalTime = 0;
    private long scanTotalTime = 0;
    private long benchmarkStartTime = 0;

    private final double readProportion;
    private final double updateProportion;
    private final double insertProportion;
    private final double scanProportion;
    private final String requestDistribution;
    private final int recordCount;
    private final int operationCount;
    private final int threadCount;
    private final BenchmarkType benchmarkType;
    private final WorkloadType workloadType;

    public static class Builder {
        private WorkloadType workloadType = WorkloadType.READ_HEAVY;
        private BenchmarkType benchmarkType = BenchmarkType.RUN;
        private String requestDistribution = "uniform";
        private int recordCount = RECORD_COUNT;
        private int operationCount = OPERATION_COUNT;
        private int threadCount = THREAD_COUNT;
        private double customReadProportion = 0.0;
        private double customUpdateProportion = 0.0;
        private double customInsertProportion = 0.0;
        private double customScanProportion = 0.0;

        public Builder workloadType(WorkloadType type) {
            this.workloadType = type;
            return this;
        }

        public Builder benchmarkType(BenchmarkType type) {
            this.benchmarkType = type;
            return this;
        }

        public Builder requestDistribution(String distribution) {
            this.requestDistribution = distribution;
            return this;
        }

        public Builder recordCount(int count) {
            this.recordCount = count;
            return this;
        }

        public Builder operationCount(int count) {
            this.operationCount = count;
            return this;
        }

        public Builder threadCount(int count) {
            this.threadCount = count;
            return this;
        }

        public Builder customProportions(double read, double update, double insert, double scan) {
            if (Math.abs(read + update + insert + scan - 1.0) > 0.0001) {
                throw new IllegalArgumentException("Operation proportions must sum to 1.0");
            }
            this.workloadType = WorkloadType.CUSTOM;
            this.customReadProportion = read;
            this.customUpdateProportion = update;
            this.customInsertProportion = insert;
            this.customScanProportion = scan;
            return this;
        }

        public BenchmarkRunner build() {
            return new BenchmarkRunner(this);
        }
    }

    private BenchmarkRunner(Builder builder) {
        this.benchmarkType = builder.benchmarkType;
        this.workloadType = builder.workloadType;
        this.requestDistribution = builder.requestDistribution;
        this.recordCount = builder.recordCount;
        this.operationCount = builder.operationCount;
        this.threadCount = builder.threadCount;

        if (builder.workloadType == WorkloadType.CUSTOM) {
            this.readProportion = builder.customReadProportion;
            this.updateProportion = builder.customUpdateProportion;
            this.insertProportion = builder.customInsertProportion;
            this.scanProportion = builder.customScanProportion;
        } else {
            this.readProportion = builder.workloadType.getReadProportion();
            this.updateProportion = builder.workloadType.getUpdateProportion();
            this.insertProportion = builder.workloadType.getInsertProportion();
            this.scanProportion = builder.workloadType.getScanProportion();
        }
    }

    public void run() {
        try {
            // Initialize Couchbase connection
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
            props.setProperty("recordcount", String.valueOf(recordCount));
            props.setProperty("operationcount", String.valueOf(operationCount));
            props.setProperty("fieldcount", "10");
            props.setProperty("fieldlength", "100");
            props.setProperty("readproportion", String.valueOf(readProportion));
            props.setProperty("updateproportion", String.valueOf(updateProportion));
            props.setProperty("scanproportion", String.valueOf(scanProportion));
            props.setProperty("insertproportion", String.valueOf(insertProportion));
            props.setProperty("requestdistribution", requestDistribution);

            // Initialize components
            Workload workload = new CoreWorkload();
            workload.init(props);

            DB db = new CouchbaseClient();
            db.setProperties(props);
            db.init();

            if (benchmarkType == BenchmarkType.LOAD) {
                System.out.println("Starting load phase...");
                loadData(db, workload, recordCount);
                System.out.println("Load phase completed.");
            } else {
                System.out.println("Starting run phase...");
                benchmarkStartTime = System.nanoTime();
                runBenchmark(db, workload, operationCount, threadCount);
                System.out.println("Run phase completed.");
                saveMetrics();
            }

            // Cleanup
            db.cleanup();
            cluster.disconnect();
            workload.cleanup();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData(DB db, Workload workload, int recordCount) throws Exception {
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

    private void runBenchmark(DB db, Workload workload, int operationCount, int threadCount) throws Exception {
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
                        if (random < readProportion) {
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
                        } else if (random < readProportion + updateProportion) {
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
                        } else if (random < readProportion + updateProportion + insertProportion) {
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

    private void saveMetrics() throws Exception {
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
        System.out.printf("Read Operations:   %d (%.1f%%)\n", readCounter.getCount(), (readCounter.getCount() * 100.0) / operationCount);
        System.out.printf("Update Operations: %d (%.1f%%)\n", updateCounter.getCount(), (updateCounter.getCount() * 100.0) / operationCount);
        System.out.printf("Insert Operations: %d (%.1f%%)\n", insertCounter.getCount(), (insertCounter.getCount() * 100.0) / operationCount);
        System.out.printf("Scan Operations:   %d (%.1f%%)\n", scanCounter.getCount(), (scanCounter.getCount() * 100.0) / operationCount);
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

    private void printTimerStats(Timer timer) {
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

    private void printThroughputStats(Timer timer) {
        System.out.printf("Mean Rate:     %.2f ops/sec\n", timer.getMeanRate());
        System.out.printf("1-Min Rate:    %.2f ops/sec\n", timer.getOneMinuteRate());
        System.out.printf("5-Min Rate:    %.2f ops/sec\n", timer.getFiveMinuteRate());
        System.out.printf("15-Min Rate:   %.2f ops/sec\n", timer.getFifteenMinuteRate());
    }

    private Map<String, Object> getTimerStats(Timer timer) {
        Map<String, Object> stats = new HashMap<>();
        Snapshot snapshot = timer.getSnapshot();
        
        stats.put("count", timer.getCount());
        stats.put("mean", snapshot.getMean());
        stats.put("median", snapshot.getMedian());
        stats.put("p75", snapshot.get75thPercentile());
        stats.put("p95", snapshot.get95thPercentile());
        stats.put("p99", snapshot.get99thPercentile());
        stats.put("p999", snapshot.get999thPercentile());
        stats.put("stddev", snapshot.getStdDev());
        stats.put("m15_rate", timer.getFifteenMinuteRate());
        stats.put("m5_rate", timer.getFiveMinuteRate());
        stats.put("m1_rate", timer.getOneMinuteRate());
        stats.put("mean_rate", timer.getMeanRate());
        
        return stats;
    }

    private Map<String, Object> getJvmMetrics() {
        Map<String, Object> jvmMetrics = new HashMap<>();
        return jvmMetrics;
    }

    public static void main(String[] args) {
        // Get benchmark type from system property
        String benchmarkTypeStr = System.getProperty("benchmark.type", "run");
        String workloadTypeStr = System.getProperty("workload.type", "read_heavy");

        BenchmarkType benchmarkType;
        try {
            benchmarkType = BenchmarkType.valueOf(benchmarkTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid benchmark type: " + benchmarkTypeStr);
            System.out.println("Valid types: load, run");
            System.exit(1);
            return;
        }

        BenchmarkRunner.Builder builder = new BenchmarkRunner.Builder()
                .benchmarkType(benchmarkType)
                .recordCount(10000)
                .operationCount(10000)
                .threadCount(1);

        try {
            WorkloadType workloadType = WorkloadType.valueOf(workloadTypeStr.toUpperCase());
            builder.workloadType(workloadType);

            // If custom workload, get proportions from system properties
            if (workloadType == WorkloadType.CUSTOM) {
                String readStr = System.getProperty("workload.read", "0.25");
                String updateStr = System.getProperty("workload.update", "0.25");
                String insertStr = System.getProperty("workload.insert", "0.25");
                String scanStr = System.getProperty("workload.scan", "0.25");

                try {
                    double read = Double.parseDouble(readStr);
                    double update = Double.parseDouble(updateStr);
                    double insert = Double.parseDouble(insertStr);
                    double scan = Double.parseDouble(scanStr);
                    builder.customProportions(read, update, insert, scan);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid proportion values. Must be numbers between 0 and 1");
                    System.exit(1);
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid workload type: " + workloadTypeStr);
            System.out.println("Valid types: read_heavy, write_heavy, read_only, write_only, mixed, custom");
            System.exit(1);
        }

        BenchmarkRunner benchmark = builder.build();
        System.out.println("Running " + benchmarkType + " benchmark with " + workloadTypeStr + " workload...");
        benchmark.run();
    }
}