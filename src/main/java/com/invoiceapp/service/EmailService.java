package com.invoiceapp.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${invoiceapp.mail.from}")
    private String from;

    public void sendInvoice(String to,
                            String subject,
                            String bodyHtml,
                            byte[] pdfBytes,
                            String fileName) {

        sendMime(to, subject, bodyHtml, pdfBytes, fileName, "application/pdf");
    }

    public void simpleNotify(String to, String subject, String text) {
        sendMime(to, subject, text, null, null, null);
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
            h.setText(body, body.contains("<"));   // html‑flag when body has tags

            if (attachmentBytes != null) {
                h.addAttachment(
                        attachmentName,
                        new ByteArrayDataSource(attachmentBytes, attachmentType));
            }
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("mail send failed", e);
        }
    }
}
