package com.invoiceapp.dto.invoice;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

//invoice item request dto

public record InvoiceItemRequest(
        @NotBlank @Size(max = 255) String description,
        @Positive int quantity,
        @Positive BigDecimal unitPrice
) {}