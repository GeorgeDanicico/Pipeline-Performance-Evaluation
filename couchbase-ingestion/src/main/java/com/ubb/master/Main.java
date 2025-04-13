package com.ubb.master;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing Couchbase configuration...");
        
        // Create Spark session for Couchbase
        SparkSession spark = SparkSession.builder()
                .appName("SparkCSVToCouchbaseApp")
                .master("local[*]")
                .config("spark.driver.cores", "1")
                .config("spark.driver.memory", "2g")
                .config("spark.executor.memory", "2g")
                // Couchbase configurations are commented out as they're not needed yet
                .config("spark.couchbase.connectionString", "localhost")
                .config("spark.couchbase.username", "admin")
                .config("spark.couchbase.password", "parola03")
                .config("spark.couchbase.bucket.census", "")
                .getOrCreate();
        
        // Register UUID generation UDF
        spark.udf().register("generateUUID", () -> java.util.UUID.randomUUID().toString(), DataTypes.StringType);
        
        // Execute Couchbase job
        CouchbaseJob couchbaseJob = new CouchbaseJob(spark, "data/us_airline_dataset.csv", 1000);
        couchbaseJob.execute();

        System.out.println("Press Enter to stop Spark session...");
        try {
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }

        spark.stop();
    }
} 