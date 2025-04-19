package com.ubb.master;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CouchbaseMain {
    public static void main(String[] args) {
        System.out.println("Initializing Couchbase configuration...");
        
        SparkSession spark = SparkConfig.createSparkSession();
        
        spark.udf().register("generateUUID", () -> java.util.UUID.randomUUID().toString(), DataTypes.StringType);
        
        CouchbaseJob couchbaseJob = new CouchbaseJob(spark, "data/yellow_tripdata_2024-06.parquet", 1000);
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