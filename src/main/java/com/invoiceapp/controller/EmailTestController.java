package com.invoiceapp.controller;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class EmailTestController {

    private final JavaMailSender mailSender;

    @Value("${invoiceapp.mail.from}")
    private String fromEmail;

    @PostMapping("/email")
    public ResponseEntity<String> sendTestEmail() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo("davidmumladze123@gmail.com"); // Change to your email
            helper.setSubject("📬 Test Email from InvoiceApp");
            helper.setText("If you're seeing this — your SMTP config is 💯", false);

            mailSender.send(message);
            return ResponseEntity.ok("Email sent!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }
}
