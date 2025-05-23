package com.ubb.master.ycsb.workload;

import com.ubb.master.ycsb.iterator.ByteIterator;

import java.util.HashMap;
import java.util.Properties;

public interface Workload {
    /**
     * Initialize the workload. This is called once per workload object,
     * before any operations are started.
     */
    void init(Properties p) throws Exception;

    /**
     * Cleanup the workload. This is called once per workload object,
     * after all operations are complete.
     */
    void cleanup() throws Exception;

    /**
     * Initialize a record for insertion.
     * 
     * @param key The key of the record to insert.
     * @param values The values to insert.
     */
    long insertInit(String key, HashMap<String, ByteIterator> values);

    /**
     * Initialize a record for update.
     * 
     * @param key The key of the record to update.
     * @param values The values to update.
     */
    void updateInit(String key, HashMap<String, ByteIterator> values);

    /**
     * Get the next key to be used for a transaction.
     * 
     * @return The next key, or null if there are no more keys.
     */
    String nextTransactionKey();
}