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

        Dataset<Row> csvData = readCSVData();
        Dataset<Row> filteredData = csvData.withColumn("document_id", callUDF("generateUUID"));

        filteredData.write()
                .format("couchbase")
                .mode("overwrite")
                .option("bucket", "census")
                .option("document.id", "document_id")
                .save();

        long endTime = System.currentTimeMillis();
        System.out.println("Couchbase Metrics:");
        System.out.println("[METRICS] Write Time: " + (endTime - startTime) + " ms");
    }
} 