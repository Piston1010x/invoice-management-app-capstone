package com.invoiceapp.dto.invoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.invoiceapp.entity.Currency;
import java.time.LocalDate;
import java.util.List;

//invoice dto
public record InvoiceRequest(
        Long clientId,
        @NotNull List<@Valid InvoiceItemRequest> items,
        @FutureOrPresent LocalDate dueDate,
        @NotNull  Currency currency,
        @NotBlank String toName,
        @NotBlank String fromName,
        @NotBlank String bankName,
        @NotBlank String iban
) {}