package com.ubb.master;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQueryException;

import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        // Create a Spark session configured to connect to a local MongoDB instance.
        SparkSession spark = SparkSession.builder()
                .appName("SparkCSVToMongoApp")
                .master("local[*]")
                .config("spark.mongodb.input.uri", "mongodb://localhost:27017/census.us_census")
                .config("spark.mongodb.output.uri", "mongodb://localhost:27017/census.us_census")
                .getOrCreate();

        // Read data from CSV file into a DataFrame
        Dataset<Row> csvData = spark.read()
                .option("header", true)
                .option("inferSchema", true)
                .csv("data/us_census_2015.csv");

        // Show the data schema and contents for validation
        csvData.printSchema();
        csvData.show();

        // Save the DataFrame to MongoDB by writing using the 'mongo' format
        csvData.write()
                .format("mongodb")
                .mode("append")
                .option("database", "census")
                .option("collection", "us_census")
                .save();

        spark.stop();
    }
}