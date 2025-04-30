package com.invoiceapp.dto.invoice;

import java.math.BigDecimal;

//invoice item response dto

public record InvoiceItemResponse(
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {}