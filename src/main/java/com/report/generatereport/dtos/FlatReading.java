package com.report.generatereport.dtos;

public class FlatReading {
    private String flatNumber;
    private long waterReading;
    private int lateFee;

    public FlatReading() {
    }

    public String getFlatNumber() {
        return this.flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public long getWaterReading() {
        return this.waterReading;
    }

    public void setWaterReading(long waterReading) {
        this.waterReading = waterReading;
    }

    public int getLateFee() {
        return this.lateFee;
    }

    public void setLateFee(int lateFee) {
        this.lateFee = lateFee;
    }
}
