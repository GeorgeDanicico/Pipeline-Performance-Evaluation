package com.benchmark.model;

import lombok.Data;
import org.bson.Document;
import java.time.LocalDateTime;

@Data
public class TaxiDocument {
    private Integer vendorId;
    private LocalDateTime pickupDateTime;
    private LocalDateTime dropoffDateTime;
    private Integer passengerCount;
    private Double tripDistance;
    private Integer ratecodeId;
    private String storeAndFwdFlag;
    private Integer puLocationId;
    private Integer doLocationId;
    private Integer paymentType;
    private Double fareAmount;
    private Double extra;
    private Double mtaTax;
    private Double tipAmount;
    private Double tollsAmount;
    private Double improvementSurcharge;
    private Double totalAmount;
    private Double congestionSurcharge;
    private Double airportFee;
    private Double cbdCongestionFee;

    public Document toDocument() {
        Document doc = new Document();
        doc.append("VendorID", vendorId)
           .append("tpep_pickup_datetime", pickupDateTime)
           .append("tpep_dropoff_datetime", dropoffDateTime)
           .append("passenger_count", passengerCount)
           .append("trip_distance", tripDistance)
           .append("RatecodeID", ratecodeId)
           .append("store_and_fwd_flag", storeAndFwdFlag)
           .append("PULocationID", puLocationId)
           .append("DOLocationID", doLocationId)
           .append("payment_type", paymentType)
           .append("fare_amount", fareAmount)
           .append("extra", extra)
           .append("mta_tax", mtaTax)
           .append("tip_amount", tipAmount)
           .append("tolls_amount", tollsAmount)
           .append("improvement_surcharge", improvementSurcharge)
           .append("total_amount", totalAmount)
           .append("congestion_surcharge", congestionSurcharge)
           .append("airport_fee", airportFee)
           .append("cbd_congestion_fee", cbdCongestionFee);
        return doc;
    }

    public static TaxiDocument fromDocument(Document doc) {
        TaxiDocument taxiTrip = new TaxiDocument();
        taxiTrip.setVendorId(doc.getInteger("VendorID"));
        taxiTrip.setPickupDateTime(doc.get("tpep_pickup_datetime", LocalDateTime.class));
        taxiTrip.setDropoffDateTime(doc.get("tpep_dropoff_datetime", LocalDateTime.class));
        taxiTrip.setPassengerCount(doc.getInteger("passenger_count"));
        taxiTrip.setTripDistance(doc.getDouble("trip_distance"));
        taxiTrip.setRatecodeId(doc.getInteger("RatecodeID"));
        taxiTrip.setStoreAndFwdFlag(doc.getString("store_and_fwd_flag"));
        taxiTrip.setPuLocationId(doc.getInteger("PULocationID"));
        taxiTrip.setDoLocationId(doc.getInteger("DOLocationID"));
        taxiTrip.setPaymentType(doc.getInteger("payment_type"));
        taxiTrip.setFareAmount(doc.getDouble("fare_amount"));
        taxiTrip.setExtra(doc.getDouble("extra"));
        taxiTrip.setMtaTax(doc.getDouble("mta_tax"));
        taxiTrip.setTipAmount(doc.getDouble("tip_amount"));
        taxiTrip.setTollsAmount(doc.getDouble("tolls_amount"));
        taxiTrip.setImprovementSurcharge(doc.getDouble("improvement_surcharge"));
        taxiTrip.setTotalAmount(doc.getDouble("total_amount"));
        taxiTrip.setCongestionSurcharge(doc.getDouble("congestion_surcharge"));
        taxiTrip.setAirportFee(doc.getDouble("airport_fee"));
        taxiTrip.setCbdCongestionFee(doc.getDouble("cbd_congestion_fee"));
        return taxiTrip;
    }
} 