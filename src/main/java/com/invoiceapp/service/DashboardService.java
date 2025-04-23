package com.invoiceapp.service;

import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository repo;

    public DashboardSnapshot snapshot() {

        long draft   = repo.countByStatus(InvoiceStatus.DRAFT);
        long sent    = repo.countByStatus(InvoiceStatus.SENT);
        long overdue = repo.countByStatus(InvoiceStatus.OVERDUE);
        long paid    = repo.countByStatus(InvoiceStatus.PAID);

        BigDecimal revenue     = repo.sumAmountByStatus(InvoiceStatus.PAID);
        BigDecimal outstanding = repo.sumAmountByStatus(InvoiceStatus.SENT)
                .add(repo.sumAmountByStatus(InvoiceStatus.OVERDUE));

        return new DashboardSnapshot(
                repo.count(), draft, sent, overdue, paid,
                revenue, outstanding, repo.grandAmount());
    }

    public record DashboardSnapshot(
            long totalInvoices,
            long draft, long sent, long overdue, long paid,
            BigDecimal revenue, BigDecimal outstanding, BigDecimal grandTotal) {}
}
