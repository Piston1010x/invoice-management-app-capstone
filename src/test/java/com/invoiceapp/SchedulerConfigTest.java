// src/test/java/com/invoiceapp/SchedulerConfigTest.java
package com.invoiceapp;

import com.invoiceapp.config.SchedulerConfig;
import com.invoiceapp.entity.*;
import com.invoiceapp.repository.InvoiceMetricRepository;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SchedulerConfigTest {

    @Mock private InvoiceRepository invoiceRepo;
    @Mock private InvoiceMetricRepository metricRepo;
    @Mock private EmailService emailService;

    @InjectMocks
    private SchedulerConfig schedulerConfig;

    private User testUser;
    private Client testClient;

    @BeforeEach
    void setup() {
        // Ensure User has equals/hashCode if comparisons are needed elsewhere
        testUser = new User(1L, "user@test.com", "pass", Role.USER, true);
        testClient = new Client(1L, "Test Client", "client@test.com", "123", testUser);
    }


    @Test
    void processOverdueInvoices_WhenNoneOverdue_ShouldLogAndDoNothingElse() {
        when(invoiceRepo.findSentAndDueOnOrBefore(eq(InvoiceStatus.SENT), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        schedulerConfig.processOverdueInvoices();

        verify(invoiceRepo).findSentAndDueOnOrBefore(eq(InvoiceStatus.SENT), any(LocalDate.class));
        verifyNoInteractions(metricRepo);
        verifyNoInteractions(emailService);
    }

    @Test
    void processOverdueInvoices_WhenSomeOverdue_ShouldUpdateStatusSendEmailsAndSaveMetrics() {
        Invoice overdueInvoice = new Invoice();
        overdueInvoice.setId(1L);
        overdueInvoice.setUser(testUser);
        overdueInvoice.setClient(testClient);
        overdueInvoice.setStatus(InvoiceStatus.SENT);
        overdueInvoice.setInvoiceNumber("OVERDUE-001");
        LocalDate dueDate = LocalDate.now().minusDays(1); // Use variable for clarity
        overdueInvoice.setDueDate(dueDate);
        overdueInvoice.setItems(new ArrayList<>());
        overdueInvoice.getItems().add(createItem(overdueInvoice, "Service", 1, "50.00"));
        // Ensure total is calculated or set if needed by email/metric logic (or rely on @PrePersist if mocking save)
        // For simplicity, let's assume the createItem helper updates the total or the service logic relies on the items.

        when(invoiceRepo.findSentAndDueOnOrBefore(eq(InvoiceStatus.SENT), any(LocalDate.class)))
                .thenReturn(List.of(overdueInvoice));

        schedulerConfig.processOverdueInvoices();

        assertThat(overdueInvoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);

        ArgumentCaptor<InvoiceMetric> metricCaptor = ArgumentCaptor.forClass(InvoiceMetric.class);
        verify(metricRepo).save(metricCaptor.capture());
        InvoiceMetric savedMetric = metricCaptor.getValue();
        assertThat(savedMetric.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        assertThat(savedMetric.getAmount()).isEqualByComparingTo("50.00");
        assertThat(savedMetric.getSnapshotDate()).isEqualTo(LocalDate.now());

        ArgumentCaptor<String> recipientCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).sendHtml(recipientCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        List<String> recipients = recipientCaptor.getAllValues();
        List<String> subjects = subjectCaptor.getAllValues();
        List<String> bodies = bodyCaptor.getAllValues();

        assertThat(recipients).containsExactlyInAnyOrder(testClient.getEmail(), testUser.getEmail());

        // Check Client Email (assuming it might also use strong tags)
        int clientEmailIndex = recipients.indexOf(testClient.getEmail());
        assertThat(subjects.get(clientEmailIndex)).isEqualTo("Overdue Invoice OVERDUE-001");
        // Example: Check specific parts if needed, including tags if they exist in sendReminderToClient
        assertThat(bodies.get(clientEmailIndex)).contains("Dear Test Client", "now overdue", "50.00", dueDate.toString());


        // Check Issuer Email - FIX Assertion
        int issuerEmailIndex = recipients.indexOf(testUser.getEmail());
        assertThat(subjects.get(issuerEmailIndex)).isEqualTo("Client Overdue Invoice OVERDUE-001");
        // FIX: Include <strong> tags in the expected substrings
        assertThat(bodies.get(issuerEmailIndex)).contains(
                "Hello user@test.com",
                "<strong>Test Client</strong>", // Expect HTML tag
                "<strong>OVERDUE-001</strong>", // Expect HTML tag
                "<strong>50.00</strong>",   // Expect HTML tag
                dueDate.toString()       // Check due date is present
        );
    }

    private InvoiceItem createItem(Invoice invoice, String desc, int qty, String unitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(desc);
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal(unitPrice));
        // Note: This helper doesn't update Invoice total. Ensure test setup or service logic handles it.
        return item;
    }
}