package com.ubb.master;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        // Initialize Spark session for MongoDB
        SparkSession mongoSpark = SparkConfig.createSparkSession("mongodb");
        mongoSpark.udf().register("generateUUID", () -> java.util.UUID.randomUUID().toString(), DataTypes.StringType);

        // Initialize Spark session for Couchbase
        SparkSession couchbaseSpark = SparkConfig.createSparkSession("couchbase");
        couchbaseSpark.udf().register("generateUUID", () -> java.util.UUID.randomUUID().toString(), DataTypes.StringType);

        // Execute MongoDB job
        MongoDBJob mongoJob = new MongoDBJob(mongoSpark, "data/us_airline_dataset.csv", 1000);
        mongoJob.execute();

        // Execute Couchbase job
        CouchbaseJob couchbaseJob = new CouchbaseJob(couchbaseSpark, "data/us_airline_dataset.csv", 1000);
        couchbaseJob.execute();

        System.out.println("Press Enter to stop Spark sessions...");
        try {
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }

        mongoSpark.stop();
        couchbaseSpark.stop();
    }
}