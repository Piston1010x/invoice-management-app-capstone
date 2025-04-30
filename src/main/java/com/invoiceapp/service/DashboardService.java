package com.invoiceapp.service;

import com.invoiceapp.dto.misc.DashboardStats;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository repo;

    //default stats from start of month → today.

    public DashboardStats getStatsFor(User user) {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end   = LocalDate.now();
        return getStatsFor(user, start, end);
    }

    //stats for a custom date range (inclusive).
    public DashboardStats getStatsFor(User user, LocalDate from, LocalDate to) {
        // counts
        long draft   = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.DRAFT,   user, from, to);
        long sent    = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.SENT,    user, from, to);
        long overdue = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.OVERDUE, user, from, to);
        long paid    = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.PAID,    user, from, to);

        // sums (null-safe)
        BigDecimal revenue     = repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.PAID,    user, from, to);
        BigDecimal sentAmount  = repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.SENT,    user, from, to);
        BigDecimal overdueAmt  = repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.OVERDUE, user, from, to);

        revenue    = revenue     != null ? revenue     : BigDecimal.ZERO;
        sentAmount = sentAmount  != null ? sentAmount  : BigDecimal.ZERO;
        overdueAmt = overdueAmt  != null ? overdueAmt  : BigDecimal.ZERO;

        BigDecimal outstanding = sentAmount.add(overdueAmt);
        long total = draft + sent + overdue + paid;

        return new DashboardStats(
                total, draft, sent, overdue, paid,
                revenue, outstanding
        );

    }
}
