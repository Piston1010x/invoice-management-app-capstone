package com.invoiceapp.dto;

import com.invoiceapp.dto.InvoiceItemResponse;
import com.invoiceapp.entity.Currency;
import com.invoiceapp.entity.InvoiceStatus;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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