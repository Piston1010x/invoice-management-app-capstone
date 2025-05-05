package com.invoiceapp.dto.invoice;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

//invoice item request dto
public record InvoiceItemRequest(
        @NotBlank @Size(max = 255) String description,

        @Positive
        int quantity,
        @Positive
        @DecimalMin(value = "0.01", message = "Unit price must be at least 0.01")
        BigDecimal unitPrice
) {}