package com.invoiceapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
        Long clientId,
        @NotNull List<@Valid InvoiceItemRequest> items,
        @FutureOrPresent LocalDate dueDate
) {}