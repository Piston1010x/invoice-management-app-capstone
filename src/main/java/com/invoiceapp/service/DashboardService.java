package com.invoiceapp.service;

import com.invoiceapp.dto.DashboardStats;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository repo;

    public DashboardStats getStatsFor(User user) {
        long draft   = repo.countByStatusAndUserAndArchivedFalse(InvoiceStatus.DRAFT, user);
        long sent    = repo.countByStatusAndUserAndArchivedFalse(InvoiceStatus.SENT, user);
        long overdue = repo.countByStatusAndUserAndArchivedFalse(InvoiceStatus.OVERDUE, user);
        long paid    = repo.countByStatusAndUserAndArchivedFalse(InvoiceStatus.PAID, user);

        BigDecimal revenue = repo.sumTotalByStatusAndUserAndArchivedFalse(InvoiceStatus.PAID, user);
        BigDecimal sentAmount = repo.sumTotalByStatusAndUserAndArchivedFalse(InvoiceStatus.SENT, user);
        BigDecimal overdueAmt = repo.sumTotalByStatusAndUserAndArchivedFalse(InvoiceStatus.OVERDUE, user);

        revenue = revenue != null ? revenue : BigDecimal.ZERO;
        sentAmount = sentAmount != null ? sentAmount : BigDecimal.ZERO;
        overdueAmt = overdueAmt != null ? overdueAmt : BigDecimal.ZERO;

        BigDecimal outstanding = sentAmount.add(overdueAmt);


        long total = draft + sent + overdue + paid;

        return new DashboardStats(
                total, draft, sent, overdue, paid,
                revenue, outstanding
        );
    }
}
