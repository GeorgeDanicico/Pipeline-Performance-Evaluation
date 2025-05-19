package com.benchmark.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomCouchbaseQueryGenerator {
    private static final Random rnd = new Random();

    // Valid discrete values
    private static final List<Integer> VENDOR_IDS      = Arrays.asList(1, 2, 6, 7);
    private static final List<Integer> RATE_CODE_IDS   = Arrays.asList(1, 2, 3, 4, 5, 6, 99);
    private static final List<Integer> PAYMENT_TYPES   = Arrays.asList(0, 1, 2, 3, 4, 5, 6);

    /**
     * Generate a random SELECT * FROM `census` WHERE ... query.
     */
    public static String randomQuery() {
        int vendorId    = pick(VENDOR_IDS);
        int rateCodeId  = pick(RATE_CODE_IDS);
        int paymentType = pick(PAYMENT_TYPES);
        int puLoc       = rndInt(1, 265);
        int doLoc       = rndInt(1, 265);

        int passengerCount = rndInt(1, 6);
        double tripDistance = rndDouble(0.1, 50.0);
        double fareAmount   = rndDouble(2.50, 200.0);

        // Build a WHERE clause combining some of these
        String where = String.join(" AND ",
                "VendorID = "      + vendorId,
                "RatecodeID = "    + rateCodeId,
                "payment_type = "  + paymentType,
                "PULocationID = "  + puLoc,
                "DOLocationID = "  + doLoc,
                "passenger_count = "     + passengerCount,
                String.format("trip_distance BETWEEN %.2f AND %.2f", tripDistance, tripDistance + 5.0),
                String.format("fare_amount BETWEEN %.2f AND %.2f", fareAmount, fareAmount + 50.0)
        );

        return "SELECT * FROM `census` WHERE " + where + " LIMIT 100;";
    }

    // Helper to pick a random element from a list
    private static <T> T pick(List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }

    // Random int in [min..max]
    private static int rndInt(int min, int max) {
        return rnd.nextInt(max - min + 1) + min;
    }

    // Random double in [min..max)
    private static double rndDouble(double min, double max) {
        return min + rnd.nextDouble() * (max - min);
    }
}