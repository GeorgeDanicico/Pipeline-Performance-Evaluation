package com.example.ycsb;

import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

public class CoreWorkload implements Workload {
    private static final String RECORD_COUNT_PROPERTY = "recordcount";
    private static final String OPERATION_COUNT_PROPERTY = "operationcount";
    private static final String FIELD_COUNT_PROPERTY = "fieldcount";
    private static final String FIELD_LENGTH_PROPERTY = "fieldlength";
    private static final String READ_PROPORTION_PROPERTY = "readproportion";
    private static final String UPDATE_PROPORTION_PROPERTY = "updateproportion";
    private static final String INSERT_PROPORTION_PROPERTY = "insertproportion";
    private static final String SCAN_PROPORTION_PROPERTY = "scanproportion";
    private static final String REQUEST_DISTRIBUTION_PROPERTY = "requestdistribution";

    private int recordCount;
    private int operationCount;
    private int fieldCount;
    private int fieldLength;
    private double readProportion;
    private double updateProportion;
    private double insertProportion;
    private double scanProportion;
    private String requestDistribution;

    private Random random;
    private int operationIndex;

    @Override
    public void init(Properties p) throws Exception {
        recordCount = Integer.parseInt(p.getProperty(RECORD_COUNT_PROPERTY, "1000"));
        operationCount = Integer.parseInt(p.getProperty(OPERATION_COUNT_PROPERTY, "1000"));
        fieldCount = Integer.parseInt(p.getProperty(FIELD_COUNT_PROPERTY, "10"));
        fieldLength = Integer.parseInt(p.getProperty(FIELD_LENGTH_PROPERTY, "100"));
        readProportion = Double.parseDouble(p.getProperty(READ_PROPORTION_PROPERTY, "0.5"));
        updateProportion = Double.parseDouble(p.getProperty(UPDATE_PROPORTION_PROPERTY, "0.5"));
        insertProportion = Double.parseDouble(p.getProperty(INSERT_PROPORTION_PROPERTY, "0"));
        scanProportion = Double.parseDouble(p.getProperty(SCAN_PROPORTION_PROPERTY, "0"));
        requestDistribution = p.getProperty(REQUEST_DISTRIBUTION_PROPERTY, "uniform");

        random = new Random();
        operationIndex = 0;
    }

    @Override
    public void cleanup() throws Exception {
        // Nothing to clean up
    }

    @Override
    public void insertInit(String key, HashMap<String, ByteIterator> values) {
        for (int i = 0; i < fieldCount; i++) {
            String fieldKey = "field" + i;
            String fieldValue = generateRandomString(fieldLength);
            values.put(fieldKey, new StringByteIterator(fieldValue));
        }
    }

    @Override
    public void updateInit(String key, HashMap<String, ByteIterator> values) {
        // For updates, we'll update a random field
        int fieldIndex = random.nextInt(fieldCount);
        String fieldKey = "field" + fieldIndex;
        String fieldValue = generateRandomString(fieldLength);
        values.put(fieldKey, new StringByteIterator(fieldValue));
    }

    @Override
    public String nextTransactionKey() {
        if (operationIndex >= operationCount) {
            return null;
        }
        operationIndex++;

        // Generate a key based on the request distribution
        int keyIndex;
        switch (requestDistribution) {
            case "zipfian":
                // Simple approximation of zipfian distribution
                keyIndex = (int) (recordCount * Math.pow(random.nextDouble(), 2));
                break;
            case "latest":
                // Most recent records are more likely to be accessed
                keyIndex = (int) (recordCount * Math.pow(random.nextDouble(), 0.5));
                break;
            case "uniform":
            default:
                keyIndex = random.nextInt(recordCount);
                break;
        }

        return String.format("user%d", keyIndex);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }
}