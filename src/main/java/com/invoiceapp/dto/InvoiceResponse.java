package com.invoiceapp.dto;

import com.invoiceapp.dto.InvoiceItemResponse;
import com.invoiceapp.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long clientId,
        String clientName,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal total,
        List<InvoiceItemResponse> items
) {}