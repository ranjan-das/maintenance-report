package com.report.generatereport;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.report.generatereport.dtos.FlatReading;
import com.report.generatereport.dtos.IndividualFlatMaintenanceReport;
import com.report.generatereport.dtos.MaintenanceInput;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.mail.MessagingException;

import com.report.generatereport.dtos.MaintenanceReport;
import com.report.generatereport.dynamodb.AwsDynamodbUtil;
import com.report.generatereport.emailservice.SimpleEmailService;

public class GenerateMaintenanceReport implements RequestHandler<MaintenanceInput, MaintenanceReport> {

    Logger logger = Logger.getLogger("GenerateMaintenanceReport");
    private static String DYNAMODB_TABLE_NAME = "MaintenanceReport";
    private static int MINIMUM_WATER_CHARGE_COLLECTED = 3200;
    private String path = "/tmp/";
    private static Font blueFont;
    private static Font normalFont;

    public GenerateMaintenanceReport() {
    }

    @Override
    public MaintenanceReport handleRequest(MaintenanceInput input, Context context) {
        MaintenanceReport currentReport = null;
        List flatReading = input.getFlatReadingList();

        try {
            MaintenanceReport returnedReport = null;
            String previousMonth = AwsDynamodbUtil.generateReportKeyForPreviousMonth();
            this.logger.info("Previous Month report: " + previousMonth);
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey("key_id", previousMonth)
                    .withConsistentRead(true);
            Table table = AwsDynamodbUtil.getDynamoDbClient().getTable(DYNAMODB_TABLE_NAME);
            Item item = table.getItem(spec);
            String returnedStr = item == null ? null : item.toJSON();
            if (returnedStr != null) {
                Map<String, String> returnedObj = (Map)(new Gson()).fromJson(returnedStr, Map.class);
                returnedReport = (MaintenanceReport)(new Gson()).fromJson((String)returnedObj.get("data"), MaintenanceReport.class);
                List<IndividualFlatMaintenanceReport> previousMonthflatMaintenanceDetails = returnedReport.getFlatMaintenanceDetailsList();
                Map<String, Long> flatToCurrentReading = (Map)flatReading.stream().collect(Collectors.toMap(FlatReading::getFlatNumber, FlatReading::getWaterReading));
                Map<String, Integer> flatToLateFee = (Map)flatReading.stream().collect(Collectors.toMap(FlatReading::getFlatNumber, FlatReading::getLateFee));
                long totalLiterConsumed = previousMonthflatMaintenanceDetails.stream().mapToLong((flat) -> {
                    return (Long)flatToCurrentReading.get(flat.getFlatNumber()) * 10L - flat.getEndReading();
                }).sum();
                long totalWaterCharge = (long)(input.getNumberOfTankers() * input.getTankerPrice()) + input.getKaveriBill() - (long)MINIMUM_WATER_CHARGE_COLLECTED;
                double perLiterCharge = this.calculatePerLiterCharge(totalLiterConsumed, totalWaterCharge);
                currentReport = this.createMaintenanceReport(previousMonthflatMaintenanceDetails, flatToLateFee, flatToCurrentReading, totalLiterConsumed, totalWaterCharge, perLiterCharge, input);
                this.persistReport(currentReport);
                this.createPdfFileFromReport(currentReport, input, flatToLateFee);
                SimpleEmailService.sendEmail(input.getRecipientEmailIds());
            }
        } catch (FileNotFoundException | DocumentException | MessagingException | AmazonServiceException var18) {
            this.logger.info("Failed to get the item: " + var18.getLocalizedMessage());
            System.out.println("Failed due to: " + var18.getLocalizedMessage());
        }

        return currentReport;
    }

    private void persistReport(MaintenanceReport currentReport) {
        ObjectMapper objMapper = new ObjectMapper();

        try {
            AwsDynamodbUtil.getDynamoDbClient().getTable(DYNAMODB_TABLE_NAME).putItem((new PutItemSpec()).withItem((new Item()).withString("key_id", currentReport.getKey()).withString("data", objMapper.writeValueAsString(currentReport))));
        } catch (JsonProcessingException var4) {
            this.logger.info("Failed to save the report");
        }

    }

    private MaintenanceReport createMaintenanceReport(List<IndividualFlatMaintenanceReport> previousMonthflatMaintenanceDetails, Map<String, Integer> flatToLateFee, Map<String, Long> flatToCurrentReading, long totalLiterConsumed, long totalWaterCharge, double perLiterCharge, MaintenanceInput input) {
        Map<String, Long> previousMonth = (Map)previousMonthflatMaintenanceDetails.stream().collect(Collectors.toMap(IndividualFlatMaintenanceReport::getFlatNumber, IndividualFlatMaintenanceReport::getEndReading));
        MaintenanceReport maintenanceReport = new MaintenanceReport();
        List<IndividualFlatMaintenanceReport> flatMaintenanceReportList = new ArrayList();
        String genKey = AwsDynamodbUtil.generateReportKey();
        maintenanceReport.setKey(genKey);
        maintenanceReport.setNumberOfTankers(input.getNumberOfTankers());
        maintenanceReport.setTankerPrice(input.getTankerPrice());
        maintenanceReport.setKaveriBill(input.getKaveriBill());
        maintenanceReport.setTotalLiterOfWaterConsumed(totalLiterConsumed);
        maintenanceReport.setTotalWaterBill(totalWaterCharge);
        maintenanceReport.setPerLiterCharge(perLiterCharge);
        Stream.of("A0", "A1", "A2", "A3", "B0", "B1", "B2", "B3", "C0", "C1", "C2", "C3", "D0", "D1", "D2", "D3").forEach((flat) -> {
            long flatConsumed = (Long)flatToCurrentReading.get(flat) * 10L - (Long)previousMonth.get(flat);
            long totalWaterBill = (long)((double)flatConsumed * perLiterCharge);
            long totalMaintenanceForFlat = totalWaterBill + (long)input.getCommonMaintenance() + (long)(Integer)flatToLateFee.get(flat);
            IndividualFlatMaintenanceReport report = new IndividualFlatMaintenanceReport();
            report.setFlatNumber(flat);
            report.setCommonMaintenance((long)input.getCommonMaintenance());
            report.setStartReading((Long)previousMonth.get(flat));
            report.setEndReading((Long)flatToCurrentReading.get(flat) * 10L);
            report.setLateFee((Integer)flatToLateFee.get(flat));
            report.setTotalLiterConsumed(flatConsumed);
            report.setWaterBill(totalWaterBill);
            report.setTotalMaintenance(totalMaintenanceForFlat);
            flatMaintenanceReportList.add(report);
        });
        maintenanceReport.setTotalMaintenanceCollected(flatMaintenanceReportList.stream().mapToLong(IndividualFlatMaintenanceReport::getTotalMaintenance).sum());
        maintenanceReport.setFlatMaintenanceDetailsList(flatMaintenanceReportList);
        return maintenanceReport;
    }

    private void createPdfFileFromReport(MaintenanceReport report, MaintenanceInput input, Map<String, Integer> flatToLateFee) throws FileNotFoundException, DocumentException {
        String BODY_HTML = "<html><head></head><body><h1>Hello!</h1><p>Please see the attached file for the maintenance details.</p></body></html>";
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(this.path + String.format("%s.pdf", AwsDynamodbUtil.generateReportKey())));
        document.open();
        this.createDocumentTitle(document);
        PdfPTable table = this.createTable(report.getPerLiterCharge(), input, report.getFlatMaintenanceDetailsList());
        Stream.of("A0", "A1", "A2", "A3", "B0", "B1", "B2", "B3", "C0", "C1", "C2", "C3", "D0", "D1", "D2", "D3").forEach((flat) -> {
            Optional<IndividualFlatMaintenanceReport> flatReport = report.getFlatMaintenanceDetailsList().stream().filter((temp) -> {
                return temp.getFlatNumber().equalsIgnoreCase(flat);
            }).findFirst();
            PdfPCell flatCell = new PdfPCell();
            flatCell.setHorizontalAlignment(1);
            flatCell.setPhrase(new Phrase(flat));
            table.addCell(flatCell);
            PdfPCell startCell = new PdfPCell();
            startCell.setHorizontalAlignment(1);
            startCell.setPhrase(new Phrase(String.valueOf(((IndividualFlatMaintenanceReport)flatReport.get()).getStartReading())));
            table.addCell(startCell);
            PdfPCell endCell = new PdfPCell();
            endCell.setHorizontalAlignment(1);
            endCell.setPhrase(new Phrase(String.valueOf(((IndividualFlatMaintenanceReport)flatReport.get()).getEndReading())));
            table.addCell(endCell);
            PdfPCell totalConsumedCell = new PdfPCell();
            totalConsumedCell.setHorizontalAlignment(1);
            totalConsumedCell.setPhrase(new Phrase(String.valueOf(((IndividualFlatMaintenanceReport)flatReport.get()).getTotalLiterConsumed())));
            table.addCell(totalConsumedCell);
            PdfPCell perLtrCell = new PdfPCell();
            perLtrCell.setHorizontalAlignment(1);
            perLtrCell.setPhrase(new Phrase(String.valueOf(report.getPerLiterCharge())));
            table.addCell(perLtrCell);
            PdfPCell totalWaterCell = new PdfPCell();
            totalWaterCell.setHorizontalAlignment(1);
            totalWaterCell.setPhrase(new Phrase(String.format("%s.00", ((IndividualFlatMaintenanceReport)flatReport.get()).getWaterBill())));
            table.addCell(totalWaterCell);
        });
        PdfPCell flatCell = new PdfPCell();
        flatCell.setHorizontalAlignment(1);
        table.addCell(flatCell);
        PdfPCell startCell = new PdfPCell();
        startCell.setHorizontalAlignment(1);
        table.addCell(startCell);
        PdfPCell endCell = new PdfPCell();
        endCell.setHorizontalAlignment(1);
        table.addCell(endCell);
        PdfPCell totalConsumedCell = new PdfPCell();
        totalConsumedCell.setHorizontalAlignment(1);
        totalConsumedCell.setBackgroundColor(BaseColor.YELLOW);
        totalConsumedCell.setPhrase(new Phrase(String.valueOf(report.getTotalLiterOfWaterConsumed())));
        table.addCell(totalConsumedCell);
        PdfPCell perLtrCell = new PdfPCell();
        perLtrCell.setHorizontalAlignment(1);
        table.addCell(perLtrCell);
        PdfPCell totalWaterCell = new PdfPCell();
        totalWaterCell.setHorizontalAlignment(1);
        totalWaterCell.setBackgroundColor(BaseColor.GREEN);
        totalWaterCell.setPhrase(new Phrase(String.format("%s.00", report.getFlatMaintenanceDetailsList().stream().mapToLong(IndividualFlatMaintenanceReport::getWaterBill).sum())));
        table.addCell(totalWaterCell);
        document.add(table);
        Paragraph paragraph = new Paragraph();
        this.addEmptyLine(paragraph, 1);
        document.add(paragraph);
        Paragraph paragraph1 = new Paragraph("Per liter charges calculated as below");
        this.addEmptyLine(paragraph1, 1);
        document.add(paragraph1);
        PdfPTable table1 = this.createPerLiterTable(input, report.getPerLiterCharge(), (double)report.getTotalLiterOfWaterConsumed());
        document.add(table1);
        document.newPage();
        this.create2ndPageDocumentTitle(document);
        this.addEmptyLine(paragraph1, 1);
        Map<String, Long> totalWaterBillMap = (Map)report.getFlatMaintenanceDetailsList().stream().collect(Collectors.toMap(IndividualFlatMaintenanceReport::getFlatNumber, IndividualFlatMaintenanceReport::getWaterBill));
        PdfPTable table2 = this.createFinalMaintenanceTable(document, totalWaterBillMap, flatToLateFee, report);
        document.add(table2);
        this.addEmptyLine(paragraph1, 1);
        this.createfooterNote(document);
        document.close();
    }

    private void createfooterNote(Document document) throws DocumentException {
        Paragraph paragraph = new Paragraph("Note: Maintenance fee shall be paid on or before 7th of the month. Late payment fee of 10.00 per day\nwill be charged for payments made after 7th.", normalFont);
        paragraph.setAlignment(1);
        this.addEmptyLine(paragraph, 1);
        document.add(paragraph);
        this.addEmptyLine(paragraph, 1);
        Paragraph paragraph1 = new Paragraph("Thank you", normalFont);
        paragraph1.setAlignment(2);
        this.addEmptyLine(paragraph1, 1);
        document.add(paragraph1);
        Paragraph paragraph2 = new Paragraph("(For Shakthi Corner Apt Owners Association)", normalFont);
        paragraph2.setAlignment(2);
        this.addEmptyLine(paragraph2, 1);
        document.add(paragraph2);
    }

    private PdfPTable createFinalMaintenanceTable(Document document, Map<String, Long> totalWaterBillMap, Map<String, Integer> flatToLateFee, MaintenanceReport report) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.0F, 2.0F, 2.0F, 1.0F, 2.0F});
        table.setWidthPercentage(60.0F);
        table.setHorizontalAlignment(1);
        Stream.of("Flat", "Common Maintenance", "Water Charge", "Late Fee", "Total Maintenance").forEach((column) -> {
            PdfPCell cell = new PdfPCell();
            cell.setHorizontalAlignment(1);
            cell.setBackgroundColor(BaseColor.YELLOW);
            cell.setPhrase(new Phrase(column));
            table.addCell(cell);
        });
        Stream.of("A0", "A1", "A2", "A3", "B0", "B1", "B2", "B3", "C0", "C1", "C2", "C3", "D0", "D1", "D2", "D3").forEach((flat) -> {
            PdfPCell flatCell = new PdfPCell();
            flatCell.setHorizontalAlignment(1);
            flatCell.setPhrase(new Phrase(flat));
            table.addCell(flatCell);
            PdfPCell commomCell = new PdfPCell();
            commomCell.setHorizontalAlignment(1);
            commomCell.setPhrase(new Phrase(String.format("%s.00", 2500)));
            table.addCell(commomCell);
            PdfPCell waterChargeCell = new PdfPCell();
            waterChargeCell.setHorizontalAlignment(1);
            waterChargeCell.setPhrase(new Phrase(String.format("%s.00", (Long)totalWaterBillMap.get(flat))));
            table.addCell(waterChargeCell);
            PdfPCell lateFeeCell = new PdfPCell();
            lateFeeCell.setHorizontalAlignment(1);
            lateFeeCell.setPhrase(new Phrase(String.format("%s.00", flatToLateFee.get(flat))));
            table.addCell(lateFeeCell);
            PdfPCell totalMaintsCell = new PdfPCell();
            totalMaintsCell.setHorizontalAlignment(1);
            totalMaintsCell.setPhrase(new Phrase(String.format("%s.00", (Long)totalWaterBillMap.get(flat) + 2500L)));
            table.addCell(totalMaintsCell);
        });
        PdfPCell flatCell = new PdfPCell();
        flatCell.setHorizontalAlignment(1);
        table.addCell(flatCell);
        PdfPCell commomCell = new PdfPCell();
        commomCell.setHorizontalAlignment(1);
        table.addCell(commomCell);
        PdfPCell waterChargeCell = new PdfPCell();
        waterChargeCell.setHorizontalAlignment(1);
        table.addCell(waterChargeCell);
        PdfPCell lateFeeCell = new PdfPCell();
        lateFeeCell.setHorizontalAlignment(1);
        table.addCell(lateFeeCell);
        PdfPCell totalMaintsCell = new PdfPCell();
        totalMaintsCell.setHorizontalAlignment(1);
        totalMaintsCell.setPhrase(new Phrase(new Phrase(String.format("%s.00", report.getTotalMaintenanceCollected()))));
        totalMaintsCell.setBackgroundColor(BaseColor.GREEN);
        table.addCell(totalMaintsCell);
        return table;
    }

    private PdfPTable createPerLiterTable(MaintenanceInput input, double literCharge, double totalConsumed) {
        PdfPTable table = new PdfPTable(new float[]{1.0F, 4.0F, 2.0F});
        table.setWidthPercentage(50.0F);
        table.setHorizontalAlignment(0);
        this.createCellNumberOfTanker(table, input);
        this.createCellTankerCost(table, input);
        this.createCellTotalTankerCost(table, input);
        this.createCellKaveriBill(table, input);
        this.createCellTotal(table, input);
        this.createCellDeduction(table, input);
        this.createCellTotalWaterConsumption(totalConsumed, table);
        this.createCellPerLiterCahrge(literCharge, table);
        return table;
    }

    private void createCellPerLiterCahrge(double literCharge, PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase("H"));
        cell.setHorizontalAlignment(0);
        table.addCell(cell);
        PdfPCell comment = new PdfPCell();
        comment.setPhrase(new Phrase("Per Litre Water Charge (G/F)"));
        comment.setHorizontalAlignment(0);
        table.addCell(comment);
        PdfPCell value = new PdfPCell();
        value.setPhrase(new Phrase(String.valueOf(literCharge)));
        value.setHorizontalAlignment(2);
        table.addCell(value);
    }

    private void createCellTotalWaterConsumption(double totalConsumed, PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase("G"));
        cell.setHorizontalAlignment(0);
        table.addCell(cell);
        PdfPCell comment = new PdfPCell();
        comment.setPhrase(new Phrase("Total Water Consumption"));
        comment.setHorizontalAlignment(0);
        table.addCell(comment);
        PdfPCell value = new PdfPCell();
        value.setPhrase(new Phrase(String.valueOf((long)totalConsumed)));
        value.setHorizontalAlignment(2);
        table.addCell(value);
    }

    private void createCellDeduction(PdfPTable table, MaintenanceInput input) {
        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase("F"));
        cell.setHorizontalAlignment(0);
        table.addCell(cell);
        PdfPCell comment = new PdfPCell();
        comment.setPhrase(new Phrase("Deduct Minimum Charge for\nCommon Water Maintenance\n(E -  3,200.00)"));
        comment.setHorizontalAlignment(0);
        table.addCell(comment);
        PdfPCell value = new PdfPCell();
        value.setPhrase(new Phrase(String.valueOf(String.format("%s.00", input.getKaveriBill() + (long)(input.getTankerPrice() * input.getNumberOfTankers()) - 3200L))));
        value.setHorizontalAlignment(2);
        table.addCell(value);
    }

    private void createCellTotal(PdfPTable table, MaintenanceInput input) {
        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase("E"));
        cell.setHorizontalAlignment(0);
        table.addCell(cell);
        PdfPCell comment = new PdfPCell();
        comment.setPhrase(new Phrase("Total (C + B)"));
        comment.setHorizontalAlignment(0);
        table.addCell(comment);
        PdfPCell value = new PdfPCell();
        value.setPhrase(new Phrase(String.valueOf(String.format("%s.00", input.getKaveriBill() + (long)(input.getTankerPrice() * input.getNumberOfTankers())))));
        value.setHorizontalAlignment(2);
        table.addCell(value);
    }

    private void createCellKaveriBill(PdfPTable table, MaintenanceInput input) {
        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase("D"));
        cell.setHorizontalAlignment(0);
        table.addCell(cell);
        PdfPCell comment = new PdfPCell();
        comment.setPhrase(new Phrase("Kaveri Bill"));
        comment.setHorizontalAlignment(0);
        table.addCell(comment);
        PdfPCell value = new PdfPCell();
        value.setPhrase(new Phrase(String.valueOf(String.format("%s.00", input.getKaveriBill()))));
        value.setHorizontalAlignment(2);
        table.addCell(value);
    }

    private void createCellTotalTankerCost(PdfPTable table, MaintenanceInput input) {
        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase("C"));
        cell.setHorizontalAlignment(0);
        table.addCell(cell);
        PdfPCell comment = new PdfPCell();
        comment.setPhrase(new Phrase("Total (A X B)"));
        comment.setHorizontalAlignment(0);
        table.addCell(comment);
        PdfPCell value = new PdfPCell();
        value.setPhrase(new Phrase(String.valueOf(String.format("%s.00", input.getTankerPrice() * input.getNumberOfTankers()))));
        value.setHorizontalAlignment(2);
        table.addCell(value);
    }

    private void createCellTankerCost(PdfPTable table, MaintenanceInput input) {
        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase("B"));
        cell.setHorizontalAlignment(0);
        table.addCell(cell);
        PdfPCell comment = new PdfPCell();
        comment.setPhrase(new Phrase("Tanker Cost"));
        comment.setHorizontalAlignment(0);
        table.addCell(comment);
        PdfPCell value = new PdfPCell();
        value.setPhrase(new Phrase(String.valueOf(String.format("%s.00", input.getTankerPrice()))));
        value.setHorizontalAlignment(2);
        table.addCell(value);
    }

    private void createCellNumberOfTanker(PdfPTable table, MaintenanceInput input) {
        PdfPCell alist = new PdfPCell();
        alist.setPhrase(new Phrase("A"));
        alist.setHorizontalAlignment(0);
        table.addCell(alist);
        PdfPCell comment = new PdfPCell();
        comment.setPhrase(new Phrase("Number of Tankers"));
        comment.setHorizontalAlignment(0);
        table.addCell(comment);
        PdfPCell value = new PdfPCell();
        value.setPhrase(new Phrase(String.valueOf(input.getNumberOfTankers())));
        value.setHorizontalAlignment(2);
        table.addCell(value);
    }

    private void create2ndPageDocumentTitle(Document document) throws DocumentException {
        Paragraph paragraph = new Paragraph("Shakthi Corner Apartment Owners Association", blueFont);
        paragraph.setAlignment(1);
        this.addEmptyLine(paragraph, 1);
        Month month = Month.of(AwsDynamodbUtil.getMonth(false));
        String monthName = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        int year = LocalDate.now().getYear();
        String tempStr = String.format("Maintenance details for %s %s as follows,", monthName, year);
        Paragraph paragraph1 = new Paragraph(tempStr, normalFont);
        paragraph1.setAlignment(1);
        this.addEmptyLine(paragraph1, 1);
        document.add(paragraph);
        document.add(paragraph1);
    }

    private void createDocumentTitle(Document document) throws DocumentException {
        Paragraph paragraph = new Paragraph("Shakthi Corner Apartment Owners Association", blueFont);
        paragraph.setAlignment(1);
        this.addEmptyLine(paragraph, 1);
        Month month = Month.of(AwsDynamodbUtil.getMonth(false));
        String monthName = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        int year = LocalDate.now().getYear();
        String tempStr = String.format("Water meter reading & water charges on individual units (as per consumption) for %s %s as follows,", monthName, year);
        Paragraph paragraph1 = new Paragraph(tempStr, normalFont);
        paragraph1.setAlignment(0);
        this.addEmptyLine(paragraph1, 1);
        document.add(paragraph);
        document.add(paragraph1);
    }

    private void addEmptyLine(Paragraph paragraph, int times) {
        for(int i = 0; i < times; ++i) {
            paragraph.add(new Paragraph(" "));
        }

    }

    private PdfPTable createTable(double perLiterCharge, MaintenanceInput input, List<IndividualFlatMaintenanceReport> flatMaintenanceDetails) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.0F, 1.2F, 1.2F, 1.3F, 1.2F, 1.2F});
        table.setWidthPercentage(100.0F);
        Stream.of("Flat", "Start Reading", "End Reading", "Total Cons. (ltr)", "Charge per ltr", "Water charge (Rounded)").forEach((x) -> {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(BaseColor.YELLOW);
            cell.setBorderWidth(1.0F);
            cell.setPhrase(new Phrase(x));
            cell.setNoWrap(false);
            cell.setHorizontalAlignment(1);
            table.addCell(cell);
        });
        return table;
    }

    private double calculatePerLiterCharge(long totalLiterConsumed, long totalWaterCharge) {
        double literPerCharge = (double)totalWaterCharge / (double)totalLiterConsumed;
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);
        String literCharge = df.format(literPerCharge);
        return Double.valueOf(literCharge);
    }

    static {
        blueFont = new Font(FontFamily.TIMES_ROMAN, 14.0F, 1, BaseColor.BLUE);
        normalFont = new Font(FontFamily.TIMES_ROMAN, 12.0F, 0);
    }
}
