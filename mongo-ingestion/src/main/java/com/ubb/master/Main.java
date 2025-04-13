package com.ubb.master;

import org.apache.spark.sql.SparkSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing MongoDB configuration...");
        
        // Create Spark session for MongoDB
        var spark = SparkConfig.createSparkSession();
        
        // Execute MongoDB job
        var mongoJob = new MongoDBJob(spark, "data/us_airline_dataset.csv", 1000);
        mongoJob.execute();

        System.out.println("Press Enter to stop Spark session...");
        try {
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }

        spark.stop();
    }
} 