package com.example.ycsb;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;

import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

public class Couchbase2Client implements DB {
    private static final String HOST_PROPERTY = "couchbase.host";
    private static final String BUCKET_PROPERTY = "couchbase.bucket";
    private static final String PASSWORD_PROPERTY = "couchbase.password";
    private static final String SYNC_MUTATION_RESPONSE_PROPERTY = "couchbase.syncMutationResponse";
    private static final String PERSIST_TO_PROPERTY = "couchbase.persistTo";
    private static final String REPLICATE_TO_PROPERTY = "couchbase.replicateTo";
    private static final String UPSERT_PROPERTY = "couchbase.upsert";
    private static final String ADHOC_PROPERTY = "couchbase.adhoc";
    private static final String KV_PROPERTY = "couchbase.kv";
    private static final String MAX_PARALLELISM_PROPERTY = "couchbase.maxParallelism";
    private static final String KV_ENDPOINTS_PROPERTY = "couchbase.kvEndpoints";
    private static final String QUERY_ENDPOINTS_PROPERTY = "couchbase.queryEndpoints";
    private static final String EPOLL_PROPERTY = "couchbase.epoll";
    private static final String BOOST_PROPERTY = "couchbase.boost";
    private static final String NETWORK_METRICS_INTERVAL_PROPERTY = "couchbase.networkMetricsInterval";
    private static final String RUNTIME_METRICS_INTERVAL_PROPERTY = "couchbase.runtimeMetricsInterval";
    private static final String DOCUMENT_EXPIRY_PROPERTY = "couchbase.documentExpiry";

    private String host;
    private String bucketName;
    private String password;
    private boolean syncMutationResponse;
    private int persistTo;
    private int replicateTo;
    private boolean upsert;
    private boolean adhoc;
    private boolean kv;
    private int maxParallelism;
    private int kvEndpoints;
    private int queryEndpoints;
    private boolean epoll;
    private int boost;
    private int networkMetricsInterval;
    private int runtimeMetricsInterval;
    private int documentExpiry;

    private Cluster cluster;
    private Bucket bucket;

    @Override
    public void init() throws Exception {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
                .connectTimeout(30000)
                .kvTimeout(10000)
                .build();

        cluster = CouchbaseCluster.create(env, host);
        bucket = cluster.openBucket(bucketName, password);
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
            JsonDocument doc = bucket.get(key);
            if (doc == null) {
                return Status.NOT_FOUND;
            }

            JsonObject content = doc.content();
            if (fields != null) {
                for (String field : fields) {
                    if (content.containsKey(field)) {
                        result.put(field, new StringByteIterator(content.getString(field)));
                    }
                }
            } else {
                for (String field : content.getNames()) {
                    result.put(field, new StringByteIterator(content.getString(field)));
                }
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
            JsonDocument doc = bucket.get(key);
            if (doc == null) {
                return Status.NOT_FOUND;
            }

            JsonObject content = doc.content();
            for (HashMap.Entry<String, ByteIterator> entry : values.entrySet()) {
                content.put(entry.getKey(), entry.getValue().toString());
            }

            bucket.upsert(doc);
            return Status.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return Status.ERROR;
        }
    }

    @Override
    public Status insert(String table, String key, HashMap<String, ByteIterator> values) {
        try {
            JsonObject content = JsonObject.create();
            for (HashMap.Entry<String, ByteIterator> entry : values.entrySet()) {
                content.put(entry.getKey(), entry.getValue().toString());
            }

            JsonDocument doc = JsonDocument.create(key, content);
            bucket.async().insert(doc);
            return Status.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return Status.ERROR;
        }
    }

    @Override
    public Status delete(String table, String key) {
        try {
            bucket.async().remove(key);
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
        password = p.getProperty(PASSWORD_PROPERTY, "");
        syncMutationResponse = Boolean.parseBoolean(p.getProperty(SYNC_MUTATION_RESPONSE_PROPERTY, "true"));
        persistTo = Integer.parseInt(p.getProperty(PERSIST_TO_PROPERTY, "0"));
        replicateTo = Integer.parseInt(p.getProperty(REPLICATE_TO_PROPERTY, "0"));
        upsert = Boolean.parseBoolean(p.getProperty(UPSERT_PROPERTY, "false"));
        adhoc = Boolean.parseBoolean(p.getProperty(ADHOC_PROPERTY, "false"));
        kv = Boolean.parseBoolean(p.getProperty(KV_PROPERTY, "true"));
        maxParallelism = Integer.parseInt(p.getProperty(MAX_PARALLELISM_PROPERTY, "1"));
        kvEndpoints = Integer.parseInt(p.getProperty(KV_ENDPOINTS_PROPERTY, "1"));
        queryEndpoints = Integer.parseInt(p.getProperty(QUERY_ENDPOINTS_PROPERTY, "5"));
        epoll = Boolean.parseBoolean(p.getProperty(EPOLL_PROPERTY, "false"));
        boost = Integer.parseInt(p.getProperty(BOOST_PROPERTY, "3"));
        networkMetricsInterval = Integer.parseInt(p.getProperty(NETWORK_METRICS_INTERVAL_PROPERTY, "0"));
        runtimeMetricsInterval = Integer.parseInt(p.getProperty(RUNTIME_METRICS_INTERVAL_PROPERTY, "0"));
        documentExpiry = Integer.parseInt(p.getProperty(DOCUMENT_EXPIRY_PROPERTY, "0"));
    }
}