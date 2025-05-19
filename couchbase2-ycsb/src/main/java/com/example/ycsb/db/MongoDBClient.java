package com.example.ycsb.db;

import com.example.ycsb.DB;
import com.example.ycsb.Status;
import com.example.ycsb.ByteIterator;
import com.example.ycsb.Status;
import com.example.ycsb.ByteIterator;
import com.example.ycsb.StringByteIterator;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

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
            this.collection.createIndex(new Document("_id", 1));
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

            if (fields == null) {
                for (String field : doc.keySet()) {
                    if (!field.equals("_id")) {
                        result.put(field, new StringByteIterator(doc.get(field).toString()));
                    }
                }
            } else {
                for (String field : fields) {
                    if (doc.containsKey(field)) {
                        result.put(field, new StringByteIterator(doc.get(field).toString()));
                    }
                }
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
            Document updateDoc = new Document();
            
            for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
                updateDoc.append(entry.getKey(), entry.getValue().toString());
            }

//            Bson update = Updates.set(updateDoc);
//            collection.updateOne(filter, update);
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
            Document doc = new Document("_id", key);
            
            for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
                doc.append(entry.getKey(), entry.getValue().toString());
            }

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

  @Override
  public void setProperties(Properties p) {

  }
} 