package com.report.generatereport.dtos;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceInput {
    private String recipientEmailIds;
    private int commonMaintenance;
    private int numberOfTankers;
    private int tankerPrice;
    private long kaveriBill;

    private List<FlatReading> flatReadingList = new ArrayList();

    public MaintenanceInput() {
    }
    public String getRecipientEmailIds() {
        return recipientEmailIds;
    }

    public void setRecipientEmailIds(String recipientEmailIds) {
        this.recipientEmailIds = recipientEmailIds;
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

    public List<FlatReading> getFlatReadingList() {
        return this.flatReadingList;
    }

    public void setFlatReadingList(List<FlatReading> flatReadingList) {
        this.flatReadingList = flatReadingList;
    }

    public int getCommonMaintenance() {
        return this.commonMaintenance;
    }

    public void setCommonMaintenance(int commonMaintenance) {
        this.commonMaintenance = commonMaintenance;
    }

}
