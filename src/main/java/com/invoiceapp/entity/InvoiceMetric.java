package com.invoiceapp.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter

//invoice metric for dashboard stats
public class InvoiceMetric {

    @Id @GeneratedValue
    private Long id;

    private LocalDate snapshotDate;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private BigDecimal amount;

    protected InvoiceMetric() {}

    public InvoiceMetric(LocalDate when, InvoiceStatus st, BigDecimal amt) {
        this.snapshotDate = when;
        this.status       = st;
        this.amount       = amt;
    }
}
