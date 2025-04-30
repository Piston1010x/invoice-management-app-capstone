package com.invoiceapp.dto.invoice;

import com.invoiceapp.entity.Currency;
import com.invoiceapp.entity.InvoiceStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

//invoice response dto

@Builder
public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long clientId,
        String clientName,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal total,
        List<InvoiceItemResponse> items,
        Currency currency,
        String toName,
        String fromName,
        String bankName,
        String iban
) {}