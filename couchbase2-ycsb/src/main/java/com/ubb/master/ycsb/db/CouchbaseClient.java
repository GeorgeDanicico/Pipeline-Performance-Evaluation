package com.ubb.master.ycsb.db;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.JsonFactory;
import com.couchbase.client.core.deps.com.fasterxml.jackson.core.JsonGenerator;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ObjectNode;
import com.couchbase.client.core.env.IoConfig;
import com.couchbase.client.core.env.IoEnvironment;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.core.retry.BestEffortRetryStrategy;
import com.couchbase.client.java.*;
import com.couchbase.client.java.codec.RawJsonTranscoder;
import com.couchbase.client.java.json.JacksonTransformers;
import com.couchbase.client.java.kv.*;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.ubb.master.ycsb.iterator.ByteIterator;
import com.ubb.master.ycsb.enums.Status;
import com.ubb.master.ycsb.iterator.StringByteIterator;

import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class CouchbaseClient implements DB {
    private static final String HOST_PROPERTY = "couchbase.host";
    private static final String BUCKET_PROPERTY = "couchbase.bucket";
    private static final String PASSWORD_PROPERTY = "couchbase.password";
    private static final String USERNAME_PROPERTY = "couchbase.username";
    private static final String CONNECT_TIMEOUT_PROPERTY = "couchbase.connectTimeout";
    private static final String KV_TIMEOUT_PROPERTY = "couchbase.kvTimeout";
    private static final String QUERY_TIMEOUT_PROPERTY = "couchbase.queryTimeout";
    private static final String MAX_PARALLELISM_PROPERTY = "couchbase.maxParallelism";
    private static final String KV_ENDPOINTS_PROPERTY = "couchbase.kvEndpoints";
    private static final String QUERY_ENDPOINTS_PROPERTY = "couchbase.queryEndpoints";

    private String host;
    private String bucketName;
    private String username;
    private String password;
    private int connectTimeout;
    private int kvTimeout;
    private int queryTimeout;
    private int maxParallelism;
    private int kvEndpoints;
    private int queryEndpoints;

    private Cluster cluster;
    private Bucket bucket;
    private Collection collection;

    @Override
    public void init() throws Exception {
        // Create cluster environment
        ClusterEnvironment env = ClusterEnvironment.builder()
                .loggingMeterConfig(config ->
                        config.enabled(true)
                                .emitInterval(Duration.ofSeconds(2)))
                .timeoutConfig(TimeoutConfig
                        .kvTimeout(Duration.ofMillis(300000))
                        .kvDurableTimeout(Duration.ofMillis(300000))
                        .queryTimeout(Duration.ofMillis(300000))
                )
                .ioConfig(IoConfig
                        .numKvConnections(200)
                )
                .ioEnvironment(IoEnvironment
                        .eventLoopThreadCount(10)
                )
                .retryStrategy(BestEffortRetryStrategy.INSTANCE)
                .build();

        // Create cluster options
        ClusterOptions options = ClusterOptions.clusterOptions(username, password)
                .environment(env);

        // Connect to cluster
        cluster = Cluster.connect(host, options);
        bucket = cluster.bucket(bucketName);
        collection = bucket.defaultCollection();
    }

    @Override
    public void cleanup() throws Exception {
        if (cluster != null) {
            cluster.disconnect();
        }
    }

    @Override
    public Status read(String table, String key, String[] fields, HashMap<String, ByteIterator> result) {
        try {
            GetResult getResult = collection.get(key);
            if (getResult == null) {
                return Status.NOT_FOUND;
            }

            return Status.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return Status.ERROR;
        }
    }

    @Override
    public Status scan(String table, String startkey, int recordcount, String[] fields,
            Vector<HashMap<String, ByteIterator>> result) {
        // Not implemented for this example
        return Status.ERROR;
    }

    @Override
    public Status update(String table, String key, HashMap<String, ByteIterator> values) {
        try {
            GetResult getResult = collection.get(key);
            if (getResult == null) {
                return Status.NOT_FOUND;
            }

            JsonObject content = getResult.contentAsObject();
            for (HashMap.Entry<String, ByteIterator> entry : values.entrySet()) {
                content.put(entry.getKey(), entry.getValue().toString());
            }

            collection.upsert(key, content);
            return Status.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return Status.ERROR;
        }
    }

    private String encode(final Map<String, ByteIterator> source) {
        Map<String, String> stringMap = StringByteIterator.getStringMap(source);
        ObjectNode node = JacksonTransformers.MAPPER.createObjectNode();
        for (Map.Entry<String, String> pair : stringMap.entrySet()) {
            node.put(pair.getKey(), pair.getValue());
        }
        JsonFactory jsonFactory = new JsonFactory();
        Writer writer = new StringWriter();
        try {
            JsonGenerator jsonGenerator = jsonFactory.createGenerator(writer);
            JacksonTransformers.MAPPER.writeTree(jsonGenerator, node);
        } catch (Exception e) {
            throw new RuntimeException("Could not encode JSON value");
        }
        return writer.toString();
    }


    @Override
    public Status insert(String table, String key, HashMap<String, ByteIterator> values) {
        try {
            collection.insert(key, encode(values), InsertOptions.insertOptions()
                    .durability(DurabilityLevel.NONE)
                    .transcoder(RawJsonTranscoder.INSTANCE));
            return Status.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return Status.ERROR;
        }
    }

    @Override
    public Status delete(String table, String key) {
        try {
            collection.remove(key);
            return Status.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return Status.ERROR;
        }
    }

    @Override
    public void setProperties(Properties p) {
        host = p.getProperty(HOST_PROPERTY, "127.0.0.1");
        bucketName = p.getProperty(BUCKET_PROPERTY, "default");
        username = p.getProperty(USERNAME_PROPERTY, "Administrator");
        password = p.getProperty(PASSWORD_PROPERTY, "");
        connectTimeout = Integer.parseInt(p.getProperty(CONNECT_TIMEOUT_PROPERTY, "30000"));
        kvTimeout = Integer.parseInt(p.getProperty(KV_TIMEOUT_PROPERTY, "10000"));
        queryTimeout = Integer.parseInt(p.getProperty(QUERY_TIMEOUT_PROPERTY, "75000"));
        maxParallelism = Integer.parseInt(p.getProperty(MAX_PARALLELISM_PROPERTY, "1"));
        kvEndpoints = Integer.parseInt(p.getProperty(KV_ENDPOINTS_PROPERTY, "1"));
        queryEndpoints = Integer.parseInt(p.getProperty(QUERY_ENDPOINTS_PROPERTY, "5"));
    }
}