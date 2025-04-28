package com.invoiceapp;

import com.invoiceapp.dto.DashboardStats;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

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
        // stub counts
        when(repo.countByStatusAndUserAndArchivedFalse(InvoiceStatus.DRAFT,   fakeUser)).thenReturn(2L);
        when(repo.countByStatusAndUserAndArchivedFalse(InvoiceStatus.SENT,    fakeUser)).thenReturn(3L);
        when(repo.countByStatusAndUserAndArchivedFalse(InvoiceStatus.OVERDUE, fakeUser)).thenReturn(4L);
        when(repo.countByStatusAndUserAndArchivedFalse(InvoiceStatus.PAID,    fakeUser)).thenReturn(5L);

        // stub sums (paid, sent, overdue)
        when(repo.sumTotalByStatusAndUserAndArchivedFalse(InvoiceStatus.PAID,    fakeUser))
                .thenReturn(new BigDecimal("100.50"));
        when(repo.sumTotalByStatusAndUserAndArchivedFalse(InvoiceStatus.SENT,    fakeUser))
                .thenReturn(new BigDecimal(" 75.25"));
        when(repo.sumTotalByStatusAndUserAndArchivedFalse(InvoiceStatus.OVERDUE, fakeUser))
                .thenReturn(new BigDecimal(" 24.75"));

        // act
        DashboardStats stats = service.getStatsFor(fakeUser);

        // assert counts
        assertThat(stats.draft()).isEqualTo(2L);
        assertThat(stats.sent()).isEqualTo(3L);
        assertThat(stats.overdue()).isEqualTo(4L);
        assertThat(stats.paid()).isEqualTo(5L);
        // total = sum of all statuses
        assertThat(stats.totalInvoices()).isEqualTo(2L + 3L + 4L + 5L);
        // revenue = paid-sum, outstanding = sent + overdue
        assertThat(stats.revenue()).isEqualByComparingTo("100.50");
        assertThat(stats.outstanding()).isEqualByComparingTo(new BigDecimal(75.25) /*75.25*/.add(new BigDecimal("24.75")));
    }
}
