package com.invoiceapp;

import com.invoiceapp.service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;
    private MimeMessage mimeMessage;

    @Value("${invoiceapp.mail.from}")
    private String from = "no-reply@invoiceapp.com";  // Default value for tests

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        mailSender   = mock(JavaMailSender.class);
        emailService = new EmailService(mailSender);

        // Using reflection to inject the 'from' value into the private field
        Field field = emailService.getClass().getDeclaredField("from");
        field.setAccessible(true);  // Make the private field accessible
        field.set(emailService, from);  // Set the 'from' value using reflection

        mimeMessage  = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendMime_whenMailSenderThrows_wrapsException() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                emailService.sendHtml("fail@user.com","Subject","<b>Body</b>")
        );
        assertTrue(ex.getMessage().contains("mail send failed"));
    }
}
