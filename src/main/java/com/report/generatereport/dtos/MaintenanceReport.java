package com.report.generatereport.dtos;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceReport {
    private String key;
    private int numberOfTankers;
    private int tankerPrice;
    private long kaveriBill;
    private long totalWaterBill;
    private double perLiterCharge;
    private long totalLiterOfWaterConsumed;
    private long totalMaintenanceCollected;
    List<IndividualFlatMaintenanceReport> flatMaintenanceDetailsList = new ArrayList();

    public MaintenanceReport() {
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getNumberOfTankers() {
        return this.numberOfTankers;
    }

    public void setNumberOfTankers(int numberOfTankers) {
        this.numberOfTankers = numberOfTankers;
    }

    public int getTankerPrice() {
        return this.tankerPrice;
    }

    public void setTankerPrice(int tankerPrice) {
        this.tankerPrice = tankerPrice;
    }

    public long getKaveriBill() {
        return this.kaveriBill;
    }

    public void setKaveriBill(long kaveriBill) {
        this.kaveriBill = kaveriBill;
    }

    public long getTotalWaterBill() {
        return this.totalWaterBill;
    }

    public void setTotalWaterBill(long totalWaterBill) {
        this.totalWaterBill = totalWaterBill;
    }

    public double getPerLiterCharge() {
        return this.perLiterCharge;
    }

    public void setPerLiterCharge(double perLiterCharge) {
        this.perLiterCharge = perLiterCharge;
    }

    public long getTotalLiterOfWaterConsumed() {
        return this.totalLiterOfWaterConsumed;
    }

    public void setTotalLiterOfWaterConsumed(long totalLiterOfWaterConsumed) {
        this.totalLiterOfWaterConsumed = totalLiterOfWaterConsumed;
    }

    public long getTotalMaintenanceCollected() {
        return this.totalMaintenanceCollected;
    }

    public void setTotalMaintenanceCollected(long totalMaintenanceCollected) {
        this.totalMaintenanceCollected = totalMaintenanceCollected;
    }

    public List<IndividualFlatMaintenanceReport> getFlatMaintenanceDetailsList() {
        return this.flatMaintenanceDetailsList;
    }

    public void setFlatMaintenanceDetailsList(List<IndividualFlatMaintenanceReport> flatMaintenanceDetailsList) {
        this.flatMaintenanceDetailsList = flatMaintenanceDetailsList;
    }
}
