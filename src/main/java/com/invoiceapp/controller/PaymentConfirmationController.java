package com.invoiceapp.controller;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PaymentConfirmationController {

    private final InvoiceRepository invoiceRepo;
    private final EmailService emailService;


    //confirm payment link
    @GetMapping("/confirm-payment/{token}")
    public ResponseEntity<String> confirm(@PathVariable String token) {

        log.info("Attempting to confirm payment for invoice with token: {}", token);
        Invoice inv = invoiceRepo.findByPaymentToken(token).orElse(null);

        if (inv == null) {
            log.error("Invalid or expired payment link for token: {}", token);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Invalid or expired link.");
        }

        if (inv.getPaymentIntentAt() != null) {
            log.info("Payment already recorded for invoice: {}. Client: {} ({})",
                    inv.getInvoiceNumber(), inv.getClient().getName(), inv.getClient().getEmail());
            return ResponseEntity.ok("We already recorded your payment. Thank you!");
        }

        inv.setPaymentIntentAt(LocalDateTime.now());
        log.info("Payment intent recorded for invoice: {}. Client: {} ({})",
                inv.getInvoiceNumber(), inv.getClient().getName(), inv.getClient().getEmail());

        emailService.simpleNotify(
                "owner@invoiceapp.local",
                "Client clicked payment link",
                """
                Client %s (%s) clicked the payment link for invoice %s.
                """
                        .formatted(inv.getClient().getName(),
                                inv.getClient().getEmail(),
                                inv.getInvoiceNumber())
        );
        log.info("Sent email notification to owner about client %s clicking payment link for invoice %s",
                inv.getClient().getName(), inv.getInvoiceNumber());

        return ResponseEntity.ok("""
            Payment noted – thank you! 
            We’ll confirm once the funds clear.
        """);
    }
}
