package com.invoiceapp.service;

import com.invoiceapp.dto.misc.DashboardStats;
import com.invoiceapp.entity.Currency;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository repo;

    public DashboardStats getStatsFor(User user, LocalDate from, LocalDate to) {
        // 1) counts
        long draft   = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.DRAFT, user, from, to);
        long sent    = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.SENT, user, from, to);
        long overdue = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.OVERDUE, user, from, to);
        long paid    = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.PAID, user, from, to);
        long totalInvoices = draft + sent + overdue + paid;

        // 2) overall sums (all currencies combined)
        BigDecimal revenue   = repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.PAID, user, from, to);
        BigDecimal sentAmt   = repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.SENT, user, from, to);
        BigDecimal overdueAmt= repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.OVERDUE, user, from, to);

        revenue    = revenue    != null ? revenue    : BigDecimal.ZERO;
        sentAmt    = sentAmt    != null ? sentAmt    : BigDecimal.ZERO;
        overdueAmt = overdueAmt != null ? overdueAmt : BigDecimal.ZERO;
        BigDecimal outstanding = sentAmt.add(overdueAmt);

        // 3) per-currency maps (String→BigDecimal)
        Map<String, BigDecimal> revByCurrency = new HashMap<>();
        Map<String, BigDecimal> outByCurrency = new HashMap<>();

        for (Currency c : Currency.values()) {
            BigDecimal paidCur = repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(
                    InvoiceStatus.PAID, user, c, from, to);
            BigDecimal sentCur = repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(
                    InvoiceStatus.SENT, user, c, from, to);
            BigDecimal odCur   = repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(
                    InvoiceStatus.OVERDUE, user, c, from, to);

            revByCurrency.put(
                    c.name(),
                    paidCur != null ? paidCur : BigDecimal.ZERO
            );
            outByCurrency.put(
                    c.name(),
                    (sentCur    != null ? sentCur    : BigDecimal.ZERO)
                            .add(odCur      != null ? odCur      : BigDecimal.ZERO)
            );
        }

        return new DashboardStats(
                totalInvoices,
                draft, sent, overdue, paid,
                revenue, outstanding,
                revByCurrency, outByCurrency
        );
    }
}
