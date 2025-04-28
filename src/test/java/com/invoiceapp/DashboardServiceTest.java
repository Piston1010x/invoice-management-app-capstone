package com.invoiceapp;

import com.invoiceapp.dto.DashboardStats;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final BigDecimal PAID_TOTAL    = new BigDecimal("100.50");
    private static final BigDecimal SENT_TOTAL    = new BigDecimal("75.25");
    private static final BigDecimal OVERDUE_TOTAL = new BigDecimal("24.75");

    @Mock
    private InvoiceRepository repo;

    @InjectMocks
    private DashboardService service;

    private User fakeUser;

    @BeforeEach
    void setUp() {
        fakeUser = new User();
        fakeUser.setId(123L);
    }

    @Test
    void getStatsFor_returnsCorrectCountsAndSums() {

        /* ── stub counts ─────────────────────────────────────────────── */
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.DRAFT),   eq(fakeUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(2L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.SENT),    eq(fakeUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(3L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.OVERDUE), eq(fakeUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(4L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.PAID),    eq(fakeUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(5L);

        /* ── stub totals ─────────────────────────────────────────────── */
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.PAID),    eq(fakeUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(PAID_TOTAL);
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.SENT),    eq(fakeUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(SENT_TOTAL);
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.OVERDUE), eq(fakeUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(OVERDUE_TOTAL);

        /* ── act ─────────────────────────────────────────────────────── */
        DashboardStats stats = service.getStatsFor(fakeUser);

        /* ── assert counts ───────────────────────────────────────────── */
        assertThat(stats.draft()).isEqualTo(2L);
        assertThat(stats.sent()).isEqualTo(3L);
        assertThat(stats.overdue()).isEqualTo(4L);
        assertThat(stats.paid()).isEqualTo(5L);
        assertThat(stats.totalInvoices()).isEqualTo(14L);

        /* ── assert sums ─────────────────────────────────────────────── */
        assertThat(stats.revenue()).isEqualByComparingTo(PAID_TOTAL);
        assertThat(stats.outstanding()).isEqualByComparingTo(SENT_TOTAL.add(OVERDUE_TOTAL));

        /* ── optional: verify interaction count ─────────────────────── */
        verify(repo, times(4))
                .countByStatusAndUserAndArchivedFalseAndIssueDateBetween(any(), eq(fakeUser), any(), any());
        verify(repo, times(3))
                .sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(any(), eq(fakeUser), any(), any());
        verifyNoMoreInteractions(repo);
    }
}
