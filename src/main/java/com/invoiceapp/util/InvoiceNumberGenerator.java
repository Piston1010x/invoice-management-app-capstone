package com.invoiceapp.util;

import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepo;
    private static final String PREFIX = "INV-";

    public String nextForUser(User user) {
        // ask the repo for the user’s current max
        int next = invoiceRepo
                .findMaxInvoiceNumberForUser(user)
                .map(this::parseSerial)
                .map(n -> n + 1)
                .orElse(1);

        return String.format("%s%05d", PREFIX, next);
    }

    private int parseSerial(String invNum) {
        // invNum == "INV-00042" 42
        return Integer.parseInt(invNum.substring(PREFIX.length()));
    }
}
