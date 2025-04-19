package com.ubb.master;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class MongoDBJob extends AbstractDatabaseJob {
    public MongoDBJob(SparkSession spark, String csvPath, int batchSize) {
        super(spark, csvPath, batchSize);
    }

    @Override
    public void execute() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

//        Dataset<Row> csvData = readCSVData();
//        long numRows = csvData.count();
//        int numPartitions = csvData.rdd().getNumPartitions();

        Dataset<Row> csvData = readParquetData();
        long numRows = csvData.count();
        int numPartitions = csvData.rdd().getNumPartitions();

        csvData.write()
                .format("mongodb")
                .mode("overwrite")
                .option("database", "census")
                .option("collection", "us_census")
                .save();

        long endTime = System.currentTimeMillis();
        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("MongoDB Metrics:");
        printMetrics(startTime, endTime, usedMemoryBefore, usedMemoryAfter, numRows, numPartitions);
    }
} 