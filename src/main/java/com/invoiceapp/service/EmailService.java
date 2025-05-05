package com.invoiceapp.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // From email
    @Value("${invoiceapp.mail.from}")
    private String from;

    public void sendInvoice(String to,
                            String subject,
                            String bodyHtml,
                            byte[] pdfBytes,
                            String fileName) {
        log.info("Preparing to send invoice email to: {} with subject: {}", to, subject);
        sendMime(to, subject, bodyHtml, pdfBytes, fileName, "application/pdf");
        log.info("Invoice email sent to: {} with subject: {}", to, subject);
    }

    // Used for payment confirmation
    public void simpleNotify(String to, String subject, String text) {
        log.info("Preparing to send payment notification email to: {} with subject: {}", to, subject);
        sendMime(to, subject, text, null, null, null);
        log.info("Payment notification email sent to: {} with subject: {}", to, subject);
    }

    // Reminder email
    public void sendHtml(String to, String subject, String htmlBody) {
        log.info("Preparing to send reminder email to: {} with subject: {}", to, subject);
        sendMime(to, subject, htmlBody, null, null, null);
        log.info("Reminder email sent to: {} with subject: {}", to, subject);
    }

    /* ---- private helper ---- */
    private void sendMime(String to,
                          String subject,
                          String body,
                          byte[] attachmentBytes,
                          String attachmentName,
                          String attachmentType) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true);

            h.setFrom(from);
            h.setTo(to);
            h.setSubject(subject);
            // Auto‐detect HTML if body contains a tag:
            h.setText(body, body != null && body.contains("<"));

            if (attachmentBytes != null) {
                log.info("Attaching file: {} of type: {}", attachmentName, attachmentType);
                h.addAttachment(
                        attachmentName,
                        new ByteArrayDataSource(attachmentBytes, attachmentType)
                );
            }

            mailSender.send(msg);
            log.info("Successfully sent email to: {} with subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to: {} with subject: {}. Error: {}", to, subject, e.getMessage());
            throw new RuntimeException("mail send failed", e);
        }
    }
}
