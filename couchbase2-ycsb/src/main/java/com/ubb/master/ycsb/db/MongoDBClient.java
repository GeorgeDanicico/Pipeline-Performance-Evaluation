package com.ubb.master.ycsb.db;

import com.mongodb.client.model.Updates;
import com.ubb.master.ycsb.enums.Status;
import com.ubb.master.ycsb.iterator.ByteIterator;
import com.ubb.master.ycsb.iterator.StringByteIterator;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static java.lang.System.getProperties;

public class MongoDBClient implements DB {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private boolean debug = false;
    private int timeout = 5000; // Default timeout in milliseconds

    @Override
    public void init() throws Exception {
        debug = Boolean.parseBoolean(getProperties().getProperty("debug", "false"));
        timeout = Integer.parseInt(getProperties().getProperty("mongodb.timeout", "5000"));

        String host = getProperties().getProperty("mongodb.host", "localhost");
        String port = getProperties().getProperty("mongodb.port", "27017");
        String db = getProperties().getProperty("mongodb.database", "ycsb");
        String collection = getProperties().getProperty("mongodb.collection", "usertable");
        String username = getProperties().getProperty("mongodb.username", "");
        String password = getProperties().getProperty("mongodb.password", "");

        // Configure MongoDB client settings
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> 
                    builder.serverSelectionTimeout(timeout, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(timeout, TimeUnit.MILLISECONDS)
                           .readTimeout(timeout, TimeUnit.MILLISECONDS))
                .applyToServerSettings(builder -> 
                    builder.heartbeatFrequency(timeout, TimeUnit.MILLISECONDS));

        // Add authentication if credentials are provided
        if (!username.isEmpty() && !password.isEmpty()) {
            settingsBuilder.credential(com.mongodb.MongoCredential.createCredential(
                username, db, password.toCharArray()));
        }

        // Create MongoDB client
        mongoClient = MongoClients.create(settingsBuilder.build());
        database = mongoClient.getDatabase(db);
        this.collection = database.getCollection(collection);

        // Create indexes if they don't exist
        try {
//            this.collection.createIndex(new Document("_id", 1));
        } catch (MongoException e) {
            if (debug) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cleanup() throws Exception {
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception e) {
                if (debug) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Status read(String table, String key, String[] fields, HashMap<String, ByteIterator> result) {
        try {
            Bson filter = Filters.eq("_id", key);
            Document doc = collection.find(filter).first();

            if (doc == null) {
                return Status.NOT_FOUND;
            }

            return Status.OK;
        } catch (MongoException e) {
            if (debug) {
                e.printStackTrace();
            }
            return Status.ERROR;
        }
    }

    @Override
    public Status scan(String table, String startkey, int recordcount, String[] fields, Vector<HashMap<String, ByteIterator>> result) {
        try {
            Bson filter = Filters.gte("_id", startkey);
            collection.find(filter)
                    .limit(recordcount)
                    .forEach(doc -> {
                        HashMap<String, ByteIterator> row = new HashMap<>();

                        if (fields == null) {
                            for (String field : doc.keySet()) {
                                if (!field.equals("_id")) {
                                    row.put(field, new StringByteIterator(doc.get(field).toString()));
                                }
                            }
                        } else {
                            for (String field : fields) {
                                if (doc.containsKey(field)) {
                                    row.put(field, new StringByteIterator(doc.get(field).toString()));
                                }
                            }
                        }

                        result.add(row);
                    });

            return Status.OK;
        } catch (MongoException e) {
            if (debug) {
                e.printStackTrace();
            }
            return Status.ERROR;
        }
    }

    @Override
    public Status update(String table, String key, HashMap<String, ByteIterator> values) {
        try {
            Bson filter = Filters.eq("_id", key);
            Document updateDoc = buildValues(values);

            Document update = new Document("$set", updateDoc);
            collection.updateOne(filter, update);
            return Status.OK;
        } catch (MongoException e) {
            if (debug) {
                e.printStackTrace();
            }
            return Status.ERROR;
        }
    }

    @Override
    public Status insert(String table, String key, HashMap<String, ByteIterator> values) {
        try {
            var doc = buildValues(values);
            doc.append("_id", key);

            collection.insertOne(doc);
            return Status.OK;
        } catch (MongoException e) {
            if (debug) {
                e.printStackTrace();
            }
            return Status.ERROR;
        }
    }

    @Override
    public Status delete(String table, String key) {
        try {
            Bson filter = Filters.eq("_id", key);
            collection.deleteOne(filter);
            return Status.OK;
        } catch (MongoException e) {
            if (debug) {
                e.printStackTrace();
            }
            return Status.ERROR;
        }
    }

    private Document buildValues(HashMap<String, ByteIterator> values) {
        Document doc = new Document();
        for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue().toString();
            
            // Convert the string value to Base64 encoded binary
            byte[] bytes = fieldValue.getBytes(StandardCharsets.UTF_8);
            String base64Value = Base64.getEncoder().encodeToString(bytes);
            
            // Create a Binary object with the Base64 encoded data
            Binary binaryValue = new Binary((byte) 0, Base64.getDecoder().decode(base64Value));
            doc.append(fieldName, binaryValue);
        }
        return doc;
    }

    private HashMap<String, ByteIterator> buildResult(Document doc) {
        HashMap<String, ByteIterator> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            if (entry.getKey().equals("_id")) {
                continue;
            }
            
            Object value = entry.getValue();
            if (value instanceof Binary) {
                // Convert Binary data back to string
                Binary binary = (Binary) value;
                String decodedValue = new String(binary.getData(), StandardCharsets.UTF_8);
                result.put(entry.getKey(), new StringByteIterator(decodedValue));
            } else {
                result.put(entry.getKey(), new StringByteIterator(value.toString()));
            }
        }
        return result;
    }

  @Override
  public void setProperties(Properties p) {

  }
} 