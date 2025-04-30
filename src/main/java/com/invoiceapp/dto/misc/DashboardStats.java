// src/main/java/com/invoiceapp/dto/misc/DashboardStats.java
package com.invoiceapp.dto.misc;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
public class DashboardStats {
    private final long totalInvoices;
    private final long draft;
    private final long sent;
    private final long overdue;
    private final long paid;

    private final BigDecimal revenue;
    private final BigDecimal outstanding;

    private final Map<String, BigDecimal> revenueByCurrency;
    private final Map<String, BigDecimal> outstandingByCurrency;

    public DashboardStats(
            long totalInvoices,
            long draft,
            long sent,
            long overdue,
            long paid,
            BigDecimal revenue,
            BigDecimal outstanding,
            Map<String, BigDecimal> revenueByCurrency,
            Map<String, BigDecimal> outstandingByCurrency
    ) {
        this.totalInvoices = totalInvoices;
        this.draft = draft;
        this.sent = sent;
        this.overdue = overdue;
        this.paid = paid;
        this.revenue = revenue;
        this.outstanding = outstanding;
        this.revenueByCurrency = revenueByCurrency;
        this.outstandingByCurrency = outstandingByCurrency;
    }
}
