package com.invoiceapp.service;

import com.invoiceapp.dto.misc.DashboardStats;
import com.invoiceapp.entity.Currency;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.invoiceapp.entity.InvoiceStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository repo;

    public DashboardStats getStatsFor(User user, LocalDate from, LocalDate to) {
        //counts
        log.info("Calculating dashboard stats for user: {} from {} to {}", user.getEmail(), from, to);
        long draft   = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                DRAFT, user, from, to);
        long sent    = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                SENT, user, from, to);
        long overdue = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.OVERDUE, user, from, to);
        long paid    = repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.PAID, user, from, to);
        long totalInvoices = draft + sent + overdue + paid;

        //overall sums (all currencies combined)
        log.debug("Draft invoices: {}, Sent invoices: {}, Overdue invoices: {}, Paid invoices: {}",
                draft, sent, overdue, paid);

        BigDecimal revenue   = repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.PAID, user, from, to);
        BigDecimal sentAmt   = repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                SENT, user, from, to);
        BigDecimal overdueAmt= repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                InvoiceStatus.OVERDUE, user, from, to);

        revenue    = revenue    != null ? revenue    : BigDecimal.ZERO;
        sentAmt    = sentAmt    != null ? sentAmt    : BigDecimal.ZERO;
        overdueAmt = overdueAmt != null ? overdueAmt : BigDecimal.ZERO;
        BigDecimal outstanding = sentAmt.add(overdueAmt);


        log.debug("Total revenue: {}, Outstanding amount: {}", revenue, outstanding);
        //per-currency maps (String→BigDecimal)
        Map<String, BigDecimal> revByCurrency = new HashMap<>();
        Map<String, BigDecimal> outByCurrency = new HashMap<>();

        for (Currency c : Currency.values()) {
            BigDecimal paidCur = repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(
                    InvoiceStatus.PAID, user, c, from, to);
            BigDecimal sentCur = repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(
                    SENT, user, c, from, to);
            BigDecimal odCur   = repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(
                    InvoiceStatus.OVERDUE, user, c, from, to);

            //Log the currency stats
            log.debug("Currency: {} - Paid: {}, Sent: {}, Overdue: {}",
                    c.name(), paidCur, sentCur, odCur);

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

        //Log the completion of the stats calculation
        log.info("Dashboard stats calculated successfully for user: {}", user.getEmail());

        return new DashboardStats(
                totalInvoices,
                draft, sent, overdue, paid,
                revenue, outstanding,
                revByCurrency, outByCurrency
        );
    }
}
