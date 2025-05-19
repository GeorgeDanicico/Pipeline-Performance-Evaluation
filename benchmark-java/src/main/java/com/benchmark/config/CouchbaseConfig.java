package com.benchmark.config;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CouchbaseConfig {

    @Value("${spring.couchbase.connection-string}")
    private String connectionString;

    @Value("${spring.couchbase.username}")
    private String username;

    @Value("${spring.couchbase.password}")
    private String password;

    @Value("${spring.couchbase.bucket-name}")
    private String bucketName;

    @Bean
    public Cluster couchbaseCluster() {
        return Cluster.connect(
            connectionString,
            ClusterOptions.clusterOptions(username, password).environment(env -> {
                env.timeoutConfig().kvScanTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().kvTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().kvDurableTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().queryTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().viewTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().analyticsTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().connectTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().disconnectTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().managementTimeout(Duration.ofSeconds(100));
                env.timeoutConfig().searchTimeout(Duration.ofSeconds(100));

            })
        );
    }

    @Bean
    public Collection couchbaseCollection(Cluster cluster) {
        return cluster.bucket(bucketName).defaultCollection();
    }
} 