package com.invoiceapp;

import com.invoiceapp.dto.misc.DashboardStats;
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

    private static final LocalDate FROM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TO   = LocalDate.of(2024, 12, 31);

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

        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.DRAFT), eq(fakeUser), any(), any())).thenReturn(2L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.SENT), eq(fakeUser), any(), any())).thenReturn(3L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.OVERDUE), eq(fakeUser), any(), any())).thenReturn(4L);
        when(repo.countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.PAID), eq(fakeUser), any(), any())).thenReturn(5L);

        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.PAID), eq(fakeUser), any(), any())).thenReturn(PAID_TOTAL);
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.SENT), eq(fakeUser), any(), any())).thenReturn(SENT_TOTAL);
        when(repo.sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
                eq(InvoiceStatus.OVERDUE), eq(fakeUser), any(), any())).thenReturn(OVERDUE_TOTAL);

        DashboardStats stats = service.getStatsFor(fakeUser, FROM, TO);

        assertThat(stats.getDraft()).isEqualTo(2L);
        assertThat(stats.getSent()).isEqualTo(3L);
        assertThat(stats.getOverdue()).isEqualTo(4L);
        assertThat(stats.getPaid()).isEqualTo(5L);
        assertThat(stats.getTotalInvoices()).isEqualTo(14L);

        assertThat(stats.getRevenue()).isEqualByComparingTo(PAID_TOTAL);
        assertThat(stats.getOutstanding()).isEqualByComparingTo(SENT_TOTAL.add(OVERDUE_TOTAL));

        verify(repo, times(4)).countByStatusAndUserAndArchivedFalseAndIssueDateBetween(any(), eq(fakeUser), any(), any());
        verify(repo, times(3)).sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(any(), eq(fakeUser), any(), any());

    }
}
