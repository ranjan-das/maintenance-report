package com.report.generatereport.dynamodb;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

import java.time.LocalDate;

public class AwsDynamodbUtil {
    public AwsDynamodbUtil() {
    }

    public static DynamoDB getDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder
                .standard()
                .build();
        return new DynamoDB(client);
    }

    public static int getMonth(boolean isPrevious) {
        int day = LocalDate.now().getDayOfMonth();
        int month;
        if (day != 29 && day != 30 && day != 31) {
            month = isPrevious ? LocalDate.now().minusMonths(2L).getMonthValue() : LocalDate.now().minusMonths(1L).getMonthValue();
        } else {
            month = isPrevious ? LocalDate.now().minusMonths(1L).getMonthValue() : LocalDate.now().getMonthValue();
        }

        return month;
    }

    public static String generateReportKey() {
        int month = getMonth(false);
        return String.format("Maintenance_%s_%s", month, getYear(month));
    }

    public static String generateReportKeyForPreviousMonth() {
        int year = LocalDate.now().getYear();
        int day = LocalDate.now().getDayOfMonth();
        int month = getMonth(true);
        return String.format("Maintenance_%s_%s", month, getYear(month));
    }

    private static Object getYear(int currentMonth) {
        return currentMonth != 11 && currentMonth != 12 ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1;
    }
}
