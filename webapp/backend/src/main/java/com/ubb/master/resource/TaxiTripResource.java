package com.ubb.master.resource;

import com.ubb.master.model.TaxiTrip;
import com.ubb.master.repository.MongoRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;

@Path("/trips")
@Produces(MediaType.APPLICATION_JSON)
public class TaxiTripResource {

    @Inject
    MongoRepository mongoRepo;

//    @Inject
//    CouchbaseTripRepository couchRepo;

    @GET
    @Path("/mongodb/time-range")
    public List<TaxiTrip> mongoTimeRange(
            @QueryParam("from") Instant from,
            @QueryParam("to")   Instant to) {

        return mongoRepo.findByPickupRange(from, to);
    }

    @GET
    @Path("/mongodb/trip-distance")
    public List<TaxiTrip> mongoTop10Distance() {

        return mongoRepo.top10LongestTrips();
    }

//    @GET
//    @Path("/couch/time-range")
//    public List<TaxiTrip> couchTimeRange(
//            @QueryParam("from") Instant from,
//            @QueryParam("to")   Instant to) {
//        return couchRepo.findByPickupRange(from, to);
//    }
}
