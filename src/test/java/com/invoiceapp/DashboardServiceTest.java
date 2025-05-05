package com.invoiceapp;

import com.invoiceapp.dto.misc.DashboardStats;
import com.invoiceapp.entity.Currency;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.invoiceapp.entity.InvoiceStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    private InvoiceRepository repo;
    private DashboardService service;
    private User user;
    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        repo = mock(InvoiceRepository.class);
        service = new DashboardService(repo);
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        from = LocalDate.of(2025, 1, 1);
        to   = LocalDate.of(2025, 1, 31);
    }

    @Test
    void getStatsFor_mixedValues() {
        // --- counts ---
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(DRAFT,   user, from, to)).thenReturn(1L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(SENT,    user, from, to)).thenReturn(2L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(OVERDUE, user, from, to)).thenReturn(3L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(PAID,    user, from, to)).thenReturn(4L);

        // --- overall sums ---
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(PAID,    user, from, to)).thenReturn(BigDecimal.valueOf(100));
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(SENT,    user, from, to)).thenReturn(BigDecimal.valueOf(50));
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(OVERDUE, user, from, to)).thenReturn(BigDecimal.valueOf(20));

        // --- per-currency sums ---
        // USD
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(PAID,    user, Currency.USD, from, to))
                .thenReturn(BigDecimal.valueOf(40));
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(SENT,    user, Currency.USD, from, to))
                .thenReturn(BigDecimal.valueOf(10));
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(OVERDUE, user, Currency.USD, from, to))
                .thenReturn(BigDecimal.valueOf(5));
        // EUR (null revenue)
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(PAID,    user, Currency.EUR, from, to))
                .thenReturn(null);
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(SENT,    user, Currency.EUR, from, to))
                .thenReturn(BigDecimal.valueOf(2));
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(OVERDUE, user, Currency.EUR, from, to))
                .thenReturn(null);
        // GBP (all null)
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(eq(PAID),    eq(user), eq(Currency.GBP), any(), any()))
                .thenReturn(null);
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(eq(SENT),    eq(user), eq(Currency.GBP), any(), any()))
                .thenReturn(null);
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(eq(OVERDUE), eq(user), eq(Currency.GBP), any(), any()))
                .thenReturn(null);
        // GEL
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(PAID,    user, Currency.GEL, from, to))
                .thenReturn(BigDecimal.valueOf(1));
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(SENT,    user, Currency.GEL, from, to))
                .thenReturn(BigDecimal.valueOf(1));
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(OVERDUE, user, Currency.GEL, from, to))
                .thenReturn(BigDecimal.valueOf(1));

        DashboardStats stats = service.getStatsFor(user, from, to);

        // verify counts
        assertEquals(1, stats.getDraft());
        assertEquals(2, stats.getSent());
        assertEquals(3, stats.getOverdue());
        assertEquals(4, stats.getPaid());
        assertEquals(1+2+3+4, stats.getTotalInvoices());

        // verify overall sums
        assertEquals(BigDecimal.valueOf(100), stats.getRevenue());
        assertEquals(BigDecimal.valueOf(50 + 20), stats.getOutstanding());

        // verify per-currency maps
        Map<String, BigDecimal> revMap = stats.getRevenueByCurrency();
        Map<String, BigDecimal> outMap = stats.getOutstandingByCurrency();

        // USD
        assertEquals(BigDecimal.valueOf(40), revMap.get("USD"));
        assertEquals(BigDecimal.valueOf(10 + 5), outMap.get("USD"));
        // EUR null→0
        assertEquals(BigDecimal.ZERO, revMap.get("EUR"));
        assertEquals(BigDecimal.valueOf(2), outMap.get("EUR"));
        // GBP all null→0
        assertEquals(BigDecimal.ZERO, revMap.get("GBP"));
        assertEquals(BigDecimal.ZERO, outMap.get("GBP"));
        // GEL
        assertEquals(BigDecimal.valueOf(1), revMap.get("GEL"));
        assertEquals(BigDecimal.valueOf(1 + 1), outMap.get("GEL"));
    }

    @Test
    void getStatsFor_allNullSums_fallbacksToZero() {
        // counts
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        // overall sums null
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(any(), any(), any(), any()))
                .thenReturn(null);
        // per-currency sums null
        when(repo.sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(any(), any(), any(), any(), any()))
                .thenReturn(null);

        DashboardStats stats = service.getStatsFor(user, from, to);

        // everything should be zero
        assertEquals(0, stats.getTotalInvoices());
        assertEquals(0, stats.getDraft());
        assertEquals(0, stats.getSent());
        assertEquals(0, stats.getOverdue());
        assertEquals(0, stats.getPaid());
        assertEquals(BigDecimal.ZERO, stats.getRevenue());
        assertEquals(BigDecimal.ZERO, stats.getOutstanding());

        // maps contain all currencies with zero values
        for (Currency c : Currency.values()) {
            assertEquals(BigDecimal.ZERO, stats.getRevenueByCurrency().get(c.name()));
            assertEquals(BigDecimal.ZERO, stats.getOutstandingByCurrency().get(c.name()));
        }
    }
}
