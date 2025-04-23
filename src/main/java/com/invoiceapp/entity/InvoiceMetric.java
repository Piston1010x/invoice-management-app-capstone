package com.invoiceapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class InvoiceMetric {

    @Id @GeneratedValue private Long id;

    private LocalDate snapshotDate;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private BigDecimal amount;

    /* JPA needs a no-arg ctor */
    protected InvoiceMetric() {}

    public InvoiceMetric(LocalDate when, InvoiceStatus st, BigDecimal amt) {
        this.snapshotDate = when;
        this.status       = st;
        this.amount       = amt;
    }

    /* getters omitted – not used anywhere yet */
}
