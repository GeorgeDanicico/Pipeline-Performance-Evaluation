package com.ubb.master;

import org.apache.spark.sql.SparkSession;

public class SparkConfig {
    public static SparkSession createSparkSession() {
        return SparkSession.builder()
                .appName("SparkCSVToMongoDBApp")
                .master("local[*]")
                .config("spark.driver.cores", "4")
                .config("spark.driver.memory", "3g")
                .config("spark.executor.memory", "3g")
                .config("spark.mongodb.input.uri", "mongodb://localhost:27017/census.us_census")
                .config("spark.mongodb.output.uri", "mongodb://localhost:27017/census.us_census")
                .config("spark.mongodb.output.maxBatchSize", 1000)
                .getOrCreate();
    }
}
