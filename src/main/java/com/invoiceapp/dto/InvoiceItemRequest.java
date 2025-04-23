package com.invoiceapp.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record InvoiceItemRequest(
        @NotBlank @Size(max = 255) String description,
        @Positive int quantity,
        @Positive BigDecimal unitPrice
) {}