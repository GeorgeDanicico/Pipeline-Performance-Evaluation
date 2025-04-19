package com.ubb.master;

import org.apache.spark.sql.SparkSession;

public class SparkConfig {
    public static SparkSession createSparkSession() {
        return SparkSession.builder()
                .appName("SparkCSVToCouchbaseApp")
                .master("local[*]")
                .config("spark.driver.cores", "3")
                .config("spark.driver.memory", "2g")
                .config("spark.executor.memory", "2g")
                .config("spark.couchbase.connectionString", "localhost")
                .config("spark.couchbase.username", "admin")
                .config("spark.couchbase.password", "parola03")
                .getOrCreate();
    }
}
