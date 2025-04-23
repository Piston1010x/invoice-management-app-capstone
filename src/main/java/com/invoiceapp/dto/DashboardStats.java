package com.invoiceapp.dto;

import java.math.BigDecimal;

// src/main/java/com/invoiceapp/dto/DashboardStats.java
public record DashboardStats(
        long totalInvoices,
        long draft, long sent, long overdue, long paid,
        BigDecimal revenue, BigDecimal outstanding) {}
