package com.ubb.master.model;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

public class TaxiTrip {

    @BsonProperty("VendorID")
    private Integer vendorID;

    @BsonProperty("tpep_pickup_datetime")
    private Instant pickupDatetime;

    @BsonProperty("tpep_dropoff_datetime")
    private Instant dropoffDatetime;

    @BsonProperty("passenger_count")
    private Double passengerCount;

    @BsonProperty("trip_distance")
    private Double tripDistance;

    @BsonProperty("RatecodeID")
    private Double ratecodeID;

    @BsonProperty("store_and_fwd_flag")
    private String storeAndFwdFlag;

    @BsonProperty("PULocationID")
    private Double puLocationID;

    @BsonProperty("DOLocationID")
    private Double doLocationID;

    @BsonProperty("payment_type")
    private Double paymentType;

    @BsonProperty("fare_amount")
    private Double fareAmount;

    @BsonProperty("extra")
    private Double extra;

    @BsonProperty("mta_tax")
    private Double mtaTax;

    @BsonProperty("tip_amount")
    private Double tipAmount;

    @BsonProperty("tolls_amount")
    private Double tollsAmount;

    @BsonProperty("improvement_surcharge")
    private Double improvementSurcharge;

    @BsonProperty("total_amount")
    private Double totalAmount;

    @BsonProperty("congestion_surcharge")
    private Double congestionSurcharge;

    @BsonProperty("airport_fee")
    private Double airportFee;

    @BsonProperty("cbd_congestion_fee")
    private Double cbdCongestionFee;

    // Required by Jackson / Quarkus
    public TaxiTrip() {}

    // Getters & Setters

    public Integer getVendorID() {
        return vendorID;
    }

    public void setVendorID(Integer vendorID) {
        this.vendorID = vendorID;
    }

    public Instant getPickupDatetime() {
        return pickupDatetime;
    }

    public void setPickupDatetime(Instant pickupDatetime) {
        this.pickupDatetime = pickupDatetime;
    }

    public Instant getDropoffDatetime() {
        return dropoffDatetime;
    }

    public void setDropoffDatetime(Instant dropoffDatetime) {
        this.dropoffDatetime = dropoffDatetime;
    }

    public Double getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(Double passengerCount) {
        this.passengerCount = passengerCount;
    }

    public Double getTripDistance() {
        return tripDistance;
    }

    public void setTripDistance(Double tripDistance) {
        this.tripDistance = tripDistance;
    }

    public Double getRatecodeID() {
        return ratecodeID;
    }

    public void setRatecodeID(Double ratecodeID) {
        this.ratecodeID = ratecodeID;
    }

    public String getStoreAndFwdFlag() {
        return storeAndFwdFlag;
    }

    public void setStoreAndFwdFlag(String storeAndFwdFlag) {
        this.storeAndFwdFlag = storeAndFwdFlag;
    }

    public Double getPuLocationID() {
        return puLocationID;
    }

    public void setPuLocationID(Double puLocationID) {
        this.puLocationID = puLocationID;
    }

    public Double getDoLocationID() {
        return doLocationID;
    }

    public void setDoLocationID(Double doLocationID) {
        this.doLocationID = doLocationID;
    }

    public Double getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(Double paymentType) {
        this.paymentType = paymentType;
    }

    public Double getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(Double fareAmount) {
        this.fareAmount = fareAmount;
    }

    public Double getExtra() {
        return extra;
    }

    public void setExtra(Double extra) {
        this.extra = extra;
    }

    public Double getMtaTax() {
        return mtaTax;
    }

    public void setMtaTax(Double mtaTax) {
        this.mtaTax = mtaTax;
    }

    public Double getTipAmount() {
        return tipAmount;
    }

    public void setTipAmount(Double tipAmount) {
        this.tipAmount = tipAmount;
    }

    public Double getTollsAmount() {
        return tollsAmount;
    }

    public void setTollsAmount(Double tollsAmount) {
        this.tollsAmount = tollsAmount;
    }

    public Double getImprovementSurcharge() {
        return improvementSurcharge;
    }

    public void setImprovementSurcharge(Double improvementSurcharge) {
        this.improvementSurcharge = improvementSurcharge;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getCongestionSurcharge() {
        return congestionSurcharge;
    }

    public void setCongestionSurcharge(Double congestionSurcharge) {
        this.congestionSurcharge = congestionSurcharge;
    }

    public Double getAirportFee() {
        return airportFee;
    }

    public void setAirportFee(Double airportFee) {
        this.airportFee = airportFee;
    }

    public Double getCbdCongestionFee() {
        return cbdCongestionFee;
    }

    public void setCbdCongestionFee(Double cbdCongestionFee) {
        this.cbdCongestionFee = cbdCongestionFee;
    }
}
