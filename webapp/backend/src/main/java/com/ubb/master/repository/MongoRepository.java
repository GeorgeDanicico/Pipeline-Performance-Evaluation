package com.ubb.master.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.ubb.master.model.TaxiTrip;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MongoRepository {
    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String databaseName;

    @ConfigProperty(name = "quarkus.mongodb.collection")
    String collectionName;

    private MongoCollection<TaxiTrip> trips;

    @PostConstruct
    void init() {
        MongoDatabase db = mongoClient.getDatabase("census");
        trips = db.getCollection("nyc_taxi", TaxiTrip.class);
    }

    /**
     * Time-range lookup: pickup_datetime ∈ [from, to)
     */
    public List<TaxiTrip> findByPickupRange(Instant from, Instant to) {
        return trips.find(
                        Filters.and(
                                Filters.gte("tpep_pickup_datetime", from),
                                Filters.lt("tpep_pickup_datetime", to)
                        )
                )
                .into(new ArrayList<>());
    }

    public List<TaxiTrip> top10LongestTrips() {
        long start = System.currentTimeMillis();
        System.out.println("Documents size: " + trips.countDocuments());
        List<TaxiTrip> results = trips
                .find()
                .sort(Sorts.descending("trip_distance"))
                .limit(10)
                .into(new ArrayList<>());
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf(
                "Top 10 Longest Trips: %d trips, took %d ms%n",
                results.size(), elapsed
        );

        return results;
    }
}
