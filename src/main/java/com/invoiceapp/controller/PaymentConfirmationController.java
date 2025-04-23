package com.invoiceapp.controller;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PaymentConfirmationController {

    private final InvoiceRepository invoiceRepo;
    private final EmailService emailService;

    @GetMapping("/confirm-payment/{token}")
    public ResponseEntity<String> confirm(@PathVariable String token) {

        Invoice inv = invoiceRepo.findByPaymentToken(token)
                .orElse(null);

        if (inv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Invalid or expired link.");
        }

        if (inv.getPaymentIntentAt() != null) {
            return ResponseEntity.ok("We already recorded your payment. Thank you!");
        }

        inv.setPaymentIntentAt(LocalDateTime.now());

        // optional: notify owner
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


        return ResponseEntity.ok("""
            Payment noted – thank you! 
            We’ll confirm once the funds clear.
        """);
    }
}
