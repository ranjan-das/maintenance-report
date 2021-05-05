package com.report.generatereport.dtos;

public class IndividualFlatMaintenanceReport {
    private String flatNumber;
    private long startReading;
    private long endReading;
    private long totalLiterConsumed;
    private long commonMaintenance;
    private long waterBill;
    private long totalMaintenance;
    private int lateFee;

    public IndividualFlatMaintenanceReport() {
    }

    public String getFlatNumber() {
        return this.flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public long getStartReading() {
        return this.startReading;
    }

    public void setStartReading(long startReading) {
        this.startReading = startReading;
    }

    public long getEndReading() {
        return this.endReading;
    }

    public void setEndReading(long endReading) {
        this.endReading = endReading;
    }

    public long getTotalLiterConsumed() {
        return this.totalLiterConsumed;
    }

    public void setTotalLiterConsumed(long totalLiterConsumed) {
        this.totalLiterConsumed = totalLiterConsumed;
    }

    public long getCommonMaintenance() {
        return this.commonMaintenance;
    }

    public void setCommonMaintenance(long commonMaintenance) {
        this.commonMaintenance = commonMaintenance;
    }

    public long getWaterBill() {
        return this.waterBill;
    }

    public void setWaterBill(long waterBill) {
        this.waterBill = waterBill;
    }

    public long getTotalMaintenance() {
        return this.totalMaintenance;
    }

    public void setTotalMaintenance(long totalMaintenance) {
        this.totalMaintenance = totalMaintenance;
    }

    public int getLateFee() {
        return this.lateFee;
    }

    public void setLateFee(int lateFee) {
        this.lateFee = lateFee;
    }
}
