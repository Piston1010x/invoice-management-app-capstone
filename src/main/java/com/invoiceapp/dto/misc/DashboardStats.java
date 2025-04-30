package com.invoiceapp.dto.misc;

import java.math.BigDecimal;

//dashboard statistics dto
public record DashboardStats(
        long totalInvoices,
        long draft, long sent, long overdue, long paid,
        BigDecimal revenue, BigDecimal outstanding) {}
