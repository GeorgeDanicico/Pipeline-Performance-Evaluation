package com.example.ycsb;

import com.example.ycsb.DB;
import com.example.ycsb.db.MongoDBClient;
import com.example.ycsb.Status;
import com.example.ycsb.ByteIterator;
import com.example.ycsb.StringByteIterator;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class MongoDBBenchmark {
    private static final Logger logger = LoggerFactory.getLogger(MongoDBBenchmark.class);
    private final int recordCount;
    private final int operationCount;
    private final int threadCount;
    private final MetricRegistry metrics;
    private final ObjectMapper mapper;
    private final WorkloadConfig workloadConfig;
    private DB db;

    private MongoDBBenchmark(Builder builder) {
        this.workloadConfig = builder.workloadConfig;
        this.recordCount = builder.recordCount;
        this.operationCount = builder.operationCount;
        this.threadCount = builder.threadCount;
        this.metrics = new MetricRegistry();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new MetricsModule(TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS, false));
    }

    public void run() throws Exception {
        logger.info("Running MongoDB benchmark...");
        
        // Initialize MongoDB client
        db = new MongoDBClient();
        Properties props = new Properties();
        props.setProperty("mongodb.host", System.getProperty("mongodb.host", "localhost"));
        props.setProperty("mongodb.port", System.getProperty("mongodb.port", "27017"));
        props.setProperty("mongodb.database", System.getProperty("mongodb.database", "ycsb"));
        props.setProperty("mongodb.collection", System.getProperty("mongodb.collection", "usertable"));
        props.setProperty("mongodb.username", System.getProperty("mongodb.username", ""));
        props.setProperty("mongodb.password", System.getProperty("mongodb.password", ""));
        props.setProperty("mongodb.timeout", System.getProperty("mongodb.timeout", "5000"));
        props.setProperty("debug", System.getProperty("debug", "false"));
        db.setProperties(props);
        db.init();

        // Create metrics
        Timer readTimer = metrics.timer("read");
        Timer updateTimer = metrics.timer("update");
        Timer insertTimer = metrics.timer("insert");
        Timer scanTimer = metrics.timer("scan");
        Counter readCounter = metrics.counter("read.count");
        Counter updateCounter = metrics.counter("update.count");
        Counter insertCounter = metrics.counter("insert.count");
        Counter scanCounter = metrics.counter("scan.count");
        Meter readMeter = metrics.meter("read.ops");
        Meter updateMeter = metrics.meter("update.ops");
        Meter insertMeter = metrics.meter("insert.ops");
        Meter scanMeter = metrics.meter("scan.ops");

        // Run benchmark
        for (int i = 0; i < operationCount; i++) {
            String key = "user" + (i % recordCount);
            double random = Math.random();
            double cumulative = 0.0;

            if (random < (cumulative += workloadConfig.getReadProportion())) {
                // Read operation
                Timer.Context context = readTimer.time();
                try {
                    Status status = db.read("usertable", key, null, new HashMap<>());
                    if (status == Status.OK) {
                        readCounter.inc();
                        readMeter.mark();
                    }
                } finally {
                    context.stop();
                }
            } else if (random < (cumulative += workloadConfig.getUpdateProportion())) {
                // Update operation
                HashMap<String, ByteIterator> values = new HashMap<>();
                values.put("field1", new StringByteIterator("newvalue1"));
                values.put("field2", new StringByteIterator("newvalue2"));

                Timer.Context context = updateTimer.time();
                try {
                    Status status = db.update("usertable", key, values);
                    if (status == Status.OK) {
                        updateCounter.inc();
                        updateMeter.mark();
                    }
                } finally {
                    context.stop();
                }
            } else if (random < (cumulative += workloadConfig.getInsertProportion())) {
                // Insert operation
                String newKey = "user" + (recordCount + i);
                HashMap<String, ByteIterator> values = new HashMap<>();
                values.put("field1", new StringByteIterator("value1"));
                values.put("field2", new StringByteIterator("value2"));

                Timer.Context context = insertTimer.time();
                try {
                    Status status = db.insert("usertable", newKey, values);
                    if (status == Status.OK) {
                        insertCounter.inc();
                        insertMeter.mark();
                    }
                } finally {
                    context.stop();
                }
            } else if (random < (cumulative += workloadConfig.getScanProportion())) {
                // Scan operation
                Timer.Context context = scanTimer.time();
                try {
                    Status status = db.scan("usertable", key, 10, null, new Vector<>());
                    if (status == Status.OK) {
                        scanCounter.inc();
                        scanMeter.mark();
                    }
                } finally {
                    context.stop();
                }
            }
        }

        // Print metrics
        ObjectNode metricsNode = mapper.createObjectNode();
        metricsNode.put("read.latency.mean", readTimer.getSnapshot().getMean());
        metricsNode.put("read.latency.p95", readTimer.getSnapshot().get95thPercentile());
        metricsNode.put("read.latency.p99", readTimer.getSnapshot().get99thPercentile());
        metricsNode.put("read.ops", readMeter.getCount());
        metricsNode.put("read.count", readCounter.getCount());

        metricsNode.put("update.latency.mean", updateTimer.getSnapshot().getMean());
        metricsNode.put("update.latency.p95", updateTimer.getSnapshot().get95thPercentile());
        metricsNode.put("update.latency.p99", updateTimer.getSnapshot().get99thPercentile());
        metricsNode.put("update.ops", updateMeter.getCount());
        metricsNode.put("update.count", updateCounter.getCount());

        metricsNode.put("insert.latency.mean", insertTimer.getSnapshot().getMean());
        metricsNode.put("insert.latency.p95", insertTimer.getSnapshot().get95thPercentile());
        metricsNode.put("insert.latency.p99", insertTimer.getSnapshot().get99thPercentile());
        metricsNode.put("insert.ops", insertMeter.getCount());
        metricsNode.put("insert.count", insertCounter.getCount());

        metricsNode.put("scan.latency.mean", scanTimer.getSnapshot().getMean());
        metricsNode.put("scan.latency.p95", scanTimer.getSnapshot().get95thPercentile());
        metricsNode.put("scan.latency.p99", scanTimer.getSnapshot().get99thPercentile());
        metricsNode.put("scan.ops", scanMeter.getCount());
        metricsNode.put("scan.count", scanCounter.getCount());

        logger.info("Benchmark results:\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metricsNode));

        // Cleanup
        db.cleanup();
    }

    public static class Builder {
        private WorkloadConfig workloadConfig = WorkloadConfig.createDefault(WorkloadType.READ_HEAVY);
        private int recordCount = 1000;
        private int operationCount = 10000;
        private int threadCount = 1;

        public Builder workloadConfig(WorkloadConfig config) {
            this.workloadConfig = config;
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

        public MongoDBBenchmark build() {
            return new MongoDBBenchmark(this);
        }
    }

    public static void main(String[] args) {
        try {
            // Get workload type from system property
            String workloadTypeStr = System.getProperty("workload.type", "read_heavy");
            WorkloadType workloadType;
            WorkloadConfig workloadConfig;

            try {
                workloadType = WorkloadType.valueOf(workloadTypeStr.toUpperCase());
                workloadConfig = WorkloadConfig.createDefault(workloadType);
            } catch (IllegalArgumentException e) {
                // Try to parse custom proportions
                try {
                    double readProp = Double.parseDouble(System.getProperty("workload.read", "0.5"));
                    double updateProp = Double.parseDouble(System.getProperty("workload.update", "0.25"));
                    double insertProp = Double.parseDouble(System.getProperty("workload.insert", "0.15"));
                    double scanProp = Double.parseDouble(System.getProperty("workload.scan", "0.1"));
                    workloadConfig = WorkloadConfig.createCustom(readProp, updateProp, insertProp, scanProp);
                } catch (NumberFormatException ex) {
                    logger.error("Invalid workload type or proportions");
                    logger.error("Usage: -Dworkload.type=[read_heavy|write_heavy|read_only|write_only|mixed]");
                    logger.error("Or use custom proportions:");
                    logger.error("-Dworkload.read=<proportion>");
                    logger.error("-Dworkload.update=<proportion>");
                    logger.error("-Dworkload.insert=<proportion>");
                    logger.error("-Dworkload.scan=<proportion>");
                    return;
                }
            }

            // Create and run benchmark
            MongoDBBenchmark benchmark = new MongoDBBenchmark.Builder()
                    .workloadConfig(workloadConfig)
                    .build();

            benchmark.run();
        } catch (Exception e) {
            logger.error("Error running benchmark", e);
        }
    }
} 