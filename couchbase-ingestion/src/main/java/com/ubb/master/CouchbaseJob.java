package com.ubb.master;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import static org.apache.spark.sql.functions.callUDF;

public class CouchbaseJob extends AbstractDatabaseJob {
    public CouchbaseJob(SparkSession spark, String csvPath, int batchSize) {
        super(spark, csvPath, batchSize);
    }

    @Override
    public void execute() {
        long startTime = System.currentTimeMillis();
        long usedMemoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

//        Dataset<Row> csvData = readCSVData();
//        Dataset<Row> filteredData = csvData.withColumn("document_id", callUDF("generateUUID"));
        Dataset<Row> csvData = readParquetData();
        Dataset<Row> filteredData =
                csvData.
                withColumn("document_id", callUDF("generateUUID"));
        long numRows = filteredData.count();
        int numPartitions = filteredData.rdd().getNumPartitions();

        filteredData.write()
                .format("couchbase.kv")
                .option("bucket", "census")
                .option("idFieldName", "document_id")
                .option("maxBatchSize", 1000)
                .save();

        long usedMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long endTime = System.currentTimeMillis();
        printMetrics(startTime, endTime, usedMemoryBefore, usedMemoryAfter, numRows, numPartitions);
        System.out.println("Couchbase Metrics:");
        System.out.println("[METRICS] Write Time: " + (endTime - startTime) + " ms");
    }
} 