package com.invoiceapp.dto;

import lombok.Getter;

import java.math.BigDecimal;
public record InvoiceItemResponse(
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {}