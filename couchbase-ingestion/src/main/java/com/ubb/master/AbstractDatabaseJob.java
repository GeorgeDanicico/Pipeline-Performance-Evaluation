package com.ubb.master;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public abstract class AbstractDatabaseJob {
    protected final SparkSession spark;
    protected final String csvPath;
    protected final int batchSize;

    protected AbstractDatabaseJob(SparkSession spark, String csvPath, int batchSize) {
        this.spark = spark;
        this.csvPath = csvPath;
        this.batchSize = batchSize;
    }

    protected Dataset<Row> readCSVData() {
        return spark.read()
                .option("header", true)
                .option("inferSchema", true)
                .option("maxRowsPerRead", batchSize)
                .csv(csvPath);
    }

    protected Dataset<Row> readParquetData() {
        return spark.read()
                .option("header", true)
                .option("inferSchema", true)
                .option("maxRowsPerRead", batchSize)
                .parquet(csvPath);
    }

    public abstract void execute();

    protected void printMetrics(long startTime, long endTime, long usedMemoryBefore, long usedMemoryAfter, long numRows, int numPartitions) {
        var timeTaken = endTime - startTime;
        var throughput = (double) numRows / (timeTaken / 1000.0); // rows per second

        System.out.println("[METRIC] Throughput: " + throughput + " rows/s");
        System.out.println("[METRIC] Time taken: " + (endTime - startTime) + " ms");
        System.out.println("[METRIC] Number of rows: " + numRows);
        System.out.println("[METRIC] Number of partitions: " + numPartitions);
        System.out.println("[METRIC] Memory used (before): " + (usedMemoryBefore / (1024 * 1024)) + " MB");
        System.out.println("[METRIC] Memory used (after): " + (usedMemoryAfter / (1024 * 1024)) + " MB");
    }
} 