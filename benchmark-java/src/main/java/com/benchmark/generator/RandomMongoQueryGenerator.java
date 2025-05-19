package com.benchmark.generator;

import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.mongodb.client.model.Filters.*;

public class RandomMongoQueryGenerator {

    private static final Random rnd = new Random();

    // Discrete value sets for enum‐like fields
    private static final List<Integer> VENDOR_IDS = Arrays.asList(1, 2, 6, 7);
    private static final List<Integer> RATE_CODE_IDS = Arrays.asList(1, 2, 3, 4, 5, 6, 99);
    private static final List<Integer> PAYMENT_TYPES = Arrays.asList(0, 1, 2, 3, 4, 5, 6);

    /**
     * Constructs a random Bson filter using equality and range predicates.
     */
    public static Bson buildRandomFilter() {
        int vendorId = pick(VENDOR_IDS);
        int rateCodeId = pick(RATE_CODE_IDS);
        int paymentType = pick(PAYMENT_TYPES);
        int puLoc = rndInt(1, 265);
        int doLoc = rndInt(1, 265);
        int passengerCount = rndInt(1, 6);
        double tripDist = rndDouble(0.1, 50.0);
        double fareAmount = rndDouble(2.5, 200.0);

        // Combine predicates with Filters.and()
        return and(
                eq("VendorID", vendorId),                                       // equals operator :contentReference[oaicite:3]{index=3}
                eq("RatecodeID", rateCodeId),
                eq("payment_type", paymentType),
                eq("PULocationID", puLoc),
                eq("DOLocationID", doLoc),
                eq("passenger_count", passengerCount),
                gte("trip_distance", tripDist),                                  // range predicate :contentReference[oaicite:4]{index=4}
                lt("trip_distance", tripDist + 5.0),
                gte("fare_amount", fareAmount),
                lt("fare_amount", fareAmount + 50.0)
        );
    }

    // Helper to pick random element from a list
    private static <T> T pick(List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }

    // Uniform random int in [min..max]
    private static int rndInt(int min, int max) {
        return rnd.nextInt(max - min + 1) + min;
    }

    // Uniform random double in [min..max)
    private static double rndDouble(double min, double max) {
        return min + rnd.nextDouble() * (max - min);
    }
}