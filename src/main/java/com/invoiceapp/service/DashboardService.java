package com.invoiceapp.service;

import com.invoiceapp.dto.DashboardStats;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import com.invoiceapp.entity.Invoice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// src/main/java/com/invoiceapp/service/DashboardService.java
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository repo;

    private BigDecimal sumTotal(InvoiceStatus status) {
        return repo.findByStatus(status).stream()
                .map(Invoice::getTotal)        // calls the getter that sums items
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public DashboardStats snapshot() {

        long draft   = repo.countByStatus(InvoiceStatus.DRAFT);
        long sent    = repo.countByStatus(InvoiceStatus.SENT);
        long overdue = repo.countByStatus(InvoiceStatus.OVERDUE);
        long paid    = repo.countByStatus(InvoiceStatus.PAID);

        BigDecimal revenue     = sumTotal(InvoiceStatus.PAID);
        BigDecimal outstanding = sumTotal(InvoiceStatus.SENT)
                .add(sumTotal(InvoiceStatus.OVERDUE));

        return new DashboardStats(
                repo.count(), draft, sent, overdue, paid,
                revenue, outstanding
        );
    }
}
