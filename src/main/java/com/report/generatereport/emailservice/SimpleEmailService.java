package com.report.generatereport.emailservice;

import com.report.generatereport.dynamodb.AwsDynamodbUtil;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Properties;

public final class SimpleEmailService {

    public static void sendEmail(String recipientEmailIds) throws MessagingException {
        String SENDER = "Shakthi Corner Apt <shakthicornersociety@gmail.com>";
        String path = "/tmp/";
        String ATTACHMENT = String.format("%s%s.pdf", path, AwsDynamodbUtil.generateReportKey());
        int PORT = 587;
        String HOST = "email-smtp.us-west-2.amazonaws.com";
        String SMTP_USERNAME = "AKIAUHYRV6VGMZNCUUHZ";
        String SMTP_PASSWORD = "BJpuhneRiJQxoO57q0aUXeoeqwgMGAPHKbr71eFsoVp1";
        Month month = Month.of(AwsDynamodbUtil.getMonth(false));
        String monthName = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String SUBJECT = String.format("Maintenance & Water Charge Details for %s-%s", monthName, LocalDate.now().getYear());
        String BODY_TEXT = "Hello,\r\nPlease see the attached file for a list of customers to contact.";
        String BODY_HTML = "<html><head></head><body><h1>Hello!</h1><p>Please see the attached file for the maintenance details.</p></body></html>";
        Properties props = System.getProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.port", Integer.valueOf(PORT));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        System.out.println("create session with props");
        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);
        message.setSubject(SUBJECT, "UTF-8");
        message.setFrom(new InternetAddress(SENDER));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmailIds));
        MimeMultipart msg_body = new MimeMultipart("alternative");
        MimeBodyPart wrap = new MimeBodyPart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(BODY_TEXT, "text/plain; charset=UTF-8");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(BODY_HTML, "text/html; charset=UTF-8");
        msg_body.addBodyPart(textPart);
        msg_body.addBodyPart(htmlPart);
        wrap.setContent(msg_body);
        MimeMultipart msg = new MimeMultipart("mixed");
        message.setContent(msg);
        msg.addBodyPart(wrap);
        MimeBodyPart att = new MimeBodyPart();
        DataSource fds = new FileDataSource(ATTACHMENT);
        att.setDataHandler(new DataHandler(fds));
        att.setFileName(fds.getName());
        msg.addBodyPart(att);
        Transport transport = session.getTransport();
        try {
            int i = 0;
            transport.connect(HOST, SMTP_USERNAME, SMTP_PASSWORD);
            while (i <= 5) {
                if (transport.isConnected()) {
                    transport.sendMessage(message, message.getAllRecipients());
                    System.out.println("Email sent!");
                    break;
                }
                Thread.sleep(1000);
               i++;
            }
        } catch (Exception var25) {
            System.out.println("Send Email Failed");
            System.err.println("Error message: " + var25.getMessage());
            var25.printStackTrace();
        }finally {
            transport.close();
            System.out.println("Closed connection");
        }
    }
}
