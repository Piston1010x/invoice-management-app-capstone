package com.invoiceapp; // Ensure correct package

import com.invoiceapp.dto.InvoiceItemRequest;
import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.dto.RecordPaymentForm;
import com.invoiceapp.entity.*;
import com.invoiceapp.repository.*;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.EmailService;
import com.invoiceapp.service.InvoicePdfService;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.util.InvoiceMapper; // Assuming static usage, or mock if made non-static
import com.invoiceapp.util.InvoiceNumberGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepo;
    @Mock ClientRepository clientRepo;
    @Mock InvoiceMetricRepository metricRepo;
    @Mock InvoiceNumberGenerator numberGenerator;
    @Mock InvoicePdfService pdfService;
    @Mock EmailService emailService;
    @Mock UserProvider userProvider;
    // No need to mock InvoiceMapper if using static methods

    @InjectMocks
    InvoiceService service;

    private User   fakeUser;
    private Client fakeClient;
    private Invoice draftInvoice;
    private Invoice sentInvoice;
    private Invoice paidInvoice;


    @BeforeEach
    void setUp() {
        fakeUser = new User(1L,"user@example.com", "hashedpass", Role.USER, true);
        fakeClient = new Client(1L, "Test Client", "client@test.com", "123", fakeUser);

        draftInvoice = new Invoice();
        draftInvoice.setId(10L);
        draftInvoice.setUser(fakeUser);
        draftInvoice.setClient(fakeClient);
        draftInvoice.setStatus(InvoiceStatus.DRAFT);
        draftInvoice.setItems(new ArrayList<>()); // Initialize items
        draftInvoice.setDueDate(LocalDate.now().plusDays(10));
        draftInvoice.setCurrency(Currency.USD);

        sentInvoice = new Invoice();
        sentInvoice.setId(11L);
        sentInvoice.setUser(fakeUser);
        sentInvoice.setClient(fakeClient);
        sentInvoice.setStatus(InvoiceStatus.SENT);
        sentInvoice.setItems(new ArrayList<>()); // Initialize items
        sentInvoice.setDueDate(LocalDate.now().minusDays(5)); // Overdue
        sentInvoice.setCurrency(Currency.EUR);
        sentInvoice.setInvoiceNumber("INV-SENT");
        sentInvoice.setPaymentToken(UUID.randomUUID().toString());


        paidInvoice = new Invoice();
        paidInvoice.setId(12L);
        paidInvoice.setUser(fakeUser);
        paidInvoice.setClient(fakeClient);
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setItems(new ArrayList<>()); // Initialize items
        paidInvoice.setDueDate(LocalDate.now().minusDays(10));
        paidInvoice.setCurrency(Currency.GBP);
        paidInvoice.setInvoiceNumber("INV-PAID");


        // Common stubbing
        lenient().when(userProvider.getCurrentUser()).thenReturn(fakeUser);
        lenient().when(clientRepo.findById(1L)).thenReturn(Optional.of(fakeClient));
        lenient().when(invoiceRepo.findById(10L)).thenReturn(Optional.of(draftInvoice));
        lenient().when(invoiceRepo.findById(11L)).thenReturn(Optional.of(sentInvoice));
        lenient().when(invoiceRepo.findById(12L)).thenReturn(Optional.of(paidInvoice));
        lenient().when(invoiceRepo.findById(999L)).thenReturn(Optional.empty()); // Not found case
    }

    // --- CREATE Tests ---
    @Test
    void create_ValidRequest_ShouldSaveAndReturnDto() {
        InvoiceRequest req = new InvoiceRequest(
                1L,
                List.of(new InvoiceItemRequest("Service", 1, BigDecimal.TEN)),
                LocalDate.now().plusDays(30), Currency.USD, "Client", "Me", "Bank", "IBAN");

        // Capture the saved entity to verify
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        // Mock save to return the entity with an ID (simulate DB save)
        when(invoiceRepo.save(invoiceCaptor.capture())).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(1L); // Assign a dummy ID
            // Ensure items have the invoice reference set if not done by mapper
            inv.getItems().forEach(item -> item.setInvoice(inv));
            return inv;
        });

        InvoiceResponse response = service.create(req);

        assertThat(response).isNotNull();
        assertThat(response.clientId()).isEqualTo(1L);
        // Check if total is calculated correctly by mapper/entity listener simulation
        assertThat(response.total()).isEqualByComparingTo("10.00");

        Invoice savedInvoice = invoiceCaptor.getValue();
        assertThat(savedInvoice.getUser()).isEqualTo(fakeUser);
        assertThat(savedInvoice.getClient()).isEqualTo(fakeClient);
        assertThat(savedInvoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT); // Default status
        assertThat(savedInvoice.getItems()).hasSize(1);
    }

    @Test
    void create_ClientNotFound_ShouldThrowException() {
        InvoiceRequest req = new InvoiceRequest(
                999L, // Non-existent client ID
                List.of(), LocalDate.now().plusDays(30), Currency.USD, "C", "M", "B", "I");
        when(clientRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Client not found");
    }

    // --- LIST Tests ---
    // Test listing with status, without status, empty results etc. (Similar to InvoiceServiceTest example provided before)
    @Test
    void list_WithStatusFilter_ShouldCallCorrectRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());
        Page<Invoice> mockPage = new PageImpl<>(List.of(sentInvoice), pageable, 1);
        when(invoiceRepo.findByStatusAndUserAndArchivedFalse(InvoiceStatus.SENT, fakeUser, pageable)).thenReturn(mockPage);

        Page<InvoiceResponse> result = service.list(Optional.of(InvoiceStatus.SENT), 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(invoiceRepo).findByStatusAndUserAndArchivedFalse(InvoiceStatus.SENT, fakeUser, pageable);
        verify(invoiceRepo, never()).findByUserAndArchivedFalse(any(), any()); // Ensure the other method wasn't called
    }


    // --- GET Tests ---
    @Test
    void get_ExistingInvoice_ShouldReturnDto() {
        InvoiceResponse response = service.get(10L); // Get the draft invoice
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void get_NonExistentInvoice_ShouldThrowException() {
        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice not found");
    }

    @Test
    void getEntity_ExistingInvoice_ShouldReturnEntity() {
        Invoice entity = service.getEntity(11L); // Get the sent invoice
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(11L);
        assertThat(entity.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    void getEntity_NonExistentInvoice_ShouldThrowException() {
        assertThatThrownBy(() -> service.getEntity(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice not found");
    }


    // --- SEND Tests ---
    @Test
    void send_DraftInvoice_ShouldUpdateStatusSendEmailAndSnapshot() {
        String generatedNumber = "INV-NEW-123";
        byte[] pdfBytes = {1, 2, 3};
        when(numberGenerator.next()).thenReturn(generatedNumber);
        when(pdfService.generate(draftInvoice)).thenReturn(pdfBytes); // Use the actual draftInvoice instance

        InvoiceResponse response = service.send(10L);

        // Assertions on the response DTO
        assertThat(response.status()).isEqualTo(InvoiceStatus.SENT);
        assertThat(response.invoiceNumber()).isEqualTo(generatedNumber);
        assertThat(response.issueDate()).isEqualTo(LocalDate.now());

        // Assertions on the entity state (captured or verified via repo save)
        assertThat(draftInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(draftInvoice.getInvoiceNumber()).isEqualTo(generatedNumber);
        assertThat(draftInvoice.getIssueDate()).isEqualTo(LocalDate.now());
        assertThat(draftInvoice.getPaymentToken()).isNotNull().hasSizeGreaterThan(10); // Check token generated

        // Verify interactions
        verify(invoiceRepo).findById(10L); // Ensure it was fetched
        // verify(invoiceRepo).save(draftInvoice); // Verify save if needed
        verify(numberGenerator).next();
        verify(pdfService).generate(draftInvoice);
        verify(emailService).sendInvoice(
                eq(fakeClient.getEmail()),
                contains(generatedNumber), // Subject contains invoice number
                contains(fakeClient.getName()), // Body contains client name
                eq(pdfBytes),
                eq(generatedNumber + ".pdf")
        );
        verify(metricRepo).save(argThat(metric -> // Verify metric details
                metric.getStatus() == InvoiceStatus.SENT &&
                        metric.getSnapshotDate().equals(LocalDate.now())
        ));
    }

    @Test
    void send_NonDraftInvoice_ShouldThrowException() {
        // Try sending the SENT invoice (ID 11L)
        assertThatThrownBy(() -> service.send(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only DRAFT can be sent");

        // Try sending the PAID invoice (ID 12L)
        assertThatThrownBy(() -> service.send(12L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only DRAFT can be sent");

        verify(emailService, never()).sendInvoice(any(), any(), any(), any(), any());
        verify(metricRepo, never()).save(any());
    }

    @Test
    void send_NonExistentInvoice_ShouldThrowException() {
        assertThatThrownBy(() -> service.send(999L))
                .isInstanceOf(EntityNotFoundException.class); // From getEntity()

        verify(emailService, never()).sendInvoice(any(), any(), any(), any(), any());
    }

    // --- MARK PAID Tests ---
    @Test
    void markPaid_SentInvoice_ShouldUpdateStatusAndSnapshot() {
        RecordPaymentForm form = new RecordPaymentForm();
        form.setPaymentDate(LocalDate.now());
        form.setPaymentMethod("Credit Card");
        form.setTransactionId("TX123");

        InvoiceResponse response = service.markPaid(11L, form); // Mark the SENT invoice as paid

        assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID); // Check entity state
        assertThat(sentInvoice.getPaymentDate()).isEqualTo(form.getPaymentDate());
        assertThat(sentInvoice.getPaymentMethod()).isEqualTo(form.getPaymentMethod());
        assertThat(sentInvoice.getTransactionId()).isEqualTo(form.getTransactionId());

        verify(metricRepo).save(argThat(metric -> metric.getStatus() == InvoiceStatus.PAID));
        // verify(invoiceRepo).save(sentInvoice); // Optional: verify save call
    }

    @Test
    void markPaid_OverdueInvoice_ShouldUpdateStatusAndSnapshot() {
        // Simulate an overdue invoice (although sentInvoice already has past due date)
        sentInvoice.setStatus(InvoiceStatus.OVERDUE); // Manually set for clarity if needed
        when(invoiceRepo.findById(11L)).thenReturn(Optional.of(sentInvoice)); // Ensure findById returns this state


        RecordPaymentForm form = new RecordPaymentForm();
        form.setPaymentDate(LocalDate.now().minusDays(1)); // Payment date in past
        form.setPaymentMethod("Bank Transfer");
        form.setTransactionId("TX456");

        InvoiceResponse response = service.markPaid(11L, form);

        assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(sentInvoice.getPaymentDate()).isEqualTo(form.getPaymentDate());
        assertThat(sentInvoice.getTransactionId()).isEqualTo(form.getTransactionId());


        verify(metricRepo).save(argThat(metric -> metric.getStatus() == InvoiceStatus.PAID));
    }


    @Test
    void markPaid_DraftInvoice_ShouldThrowException() {
        RecordPaymentForm form = new RecordPaymentForm();
        assertThatThrownBy(() -> service.markPaid(10L, form)) // Try marking DRAFT invoice
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only SENT/OVERDUE can be paid");
        verify(metricRepo, never()).save(any());
    }

    @Test
    void markPaid_AlreadyPaidInvoice_ShouldThrowException() {
        RecordPaymentForm form = new RecordPaymentForm();
        assertThatThrownBy(() -> service.markPaid(12L, form)) // Try marking PAID invoice again
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only SENT/OVERDUE can be paid");
        verify(metricRepo, never()).save(any());
    }


    // --- UPDATE Tests ---
    // Add tests for update: successful update, attempt to update non-draft, client not found during update

    // --- DELETE/ARCHIVE Tests ---
    @Test
    void delete_ExistingInvoice_ShouldSetArchivedFlag() {
        service.delete(10L); // Delete draft invoice

        assertThat(draftInvoice.isArchived()).isTrue();
        // verify(invoiceRepo).save(draftInvoice); // Verify save if needed
    }

    @Test
    void archive_ExistingInvoice_ShouldSetArchivedFlag() {
        service.archive(11L); // Archive sent invoice

        assertThat(sentInvoice.isArchived()).isTrue();
        // verify(invoiceRepo).save(sentInvoice); // Verify save if needed
    }

    @Test
    void delete_NonExistentInvoice_ShouldThrowException() {
        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- MARK OVERDUE Test ---
    @Test
    void markOverdue_ShouldTransitionSentPastDueInvoices() {
        // Arrange: Prepare invoices - one past due, one future due, one already overdue
        Invoice sentPastDue = new Invoice();
        sentPastDue.setId(20L); sentPastDue.setUser(fakeUser); sentPastDue.setClient(fakeClient);
        sentPastDue.setStatus(InvoiceStatus.SENT); sentPastDue.setDueDate(LocalDate.now().minusDays(1));
        sentPastDue.setItems(new ArrayList<>()); // Needed for snapshot

        Invoice sentFutureDue = new Invoice();
        sentFutureDue.setId(21L); sentFutureDue.setUser(fakeUser); sentFutureDue.setClient(fakeClient);
        sentFutureDue.setStatus(InvoiceStatus.SENT); sentFutureDue.setDueDate(LocalDate.now().plusDays(1));

        Invoice alreadyOverdue = new Invoice();
        alreadyOverdue.setId(22L); alreadyOverdue.setUser(fakeUser); alreadyOverdue.setClient(fakeClient);
        alreadyOverdue.setStatus(InvoiceStatus.OVERDUE); alreadyOverdue.setDueDate(LocalDate.now().minusDays(10));


        // Mock repository to return only the active SENT invoices
        when(invoiceRepo.findActive(InvoiceStatus.SENT)).thenReturn(List.of(sentPastDue, sentFutureDue));

        // Act
        int count = service.markOverdue();

        // Assert
        assertThat(count).isEqualTo(1); // Only one should have been transitioned
        assertThat(sentPastDue.getStatus()).isEqualTo(InvoiceStatus.OVERDUE); // This one changed
        assertThat(sentFutureDue.getStatus()).isEqualTo(InvoiceStatus.SENT); // This one didn't
        assertThat(alreadyOverdue.getStatus()).isEqualTo(InvoiceStatus.OVERDUE); // Unaffected

        // Verify metric snapshot was called only for the transitioned invoice
        verify(metricRepo, times(1)).save(argThat(m -> m.getStatus() == InvoiceStatus.OVERDUE));
        // Verify save call if needed
        // verify(invoiceRepo).save(sentPastDue);
    }


    // --- REVERT PAYMENT Tests ---
    @Test
    void revertPaymentStatus_PaidInvoice_ShouldRevertToSentAndClearFields() {
        // Setup initial paid state (optional if already done in @BeforeEach)
        paidInvoice.setPaymentDate(LocalDate.now().minusDays(2));
        paidInvoice.setPaymentMethod("Cash");
        paidInvoice.setPaymentAmountRecorded(BigDecimal.TEN);
        paidInvoice.setPaymentNotes("Paid notes");
        paidInvoice.setTransactionId("TX-PAID");


        InvoiceResponse response = service.revertPaymentStatus(12L);

        assertThat(response.status()).isEqualTo(InvoiceStatus.SENT);
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(paidInvoice.getPaymentDate()).isNull();
        assertThat(paidInvoice.getPaymentMethod()).isNull();
        assertThat(paidInvoice.getPaymentAmountRecorded()).isNull();
        assertThat(paidInvoice.getPaymentNotes()).isNull();
        assertThat(paidInvoice.getTransactionId()).isNull(); // Should also clear txn id? Decide based on requirements. Assuming yes here.


        // verify(invoiceRepo).save(paidInvoice); // Verify save
        verify(metricRepo, never()).save(any()); // Should not snapshot on revert
    }

    @Test
    void revertPaymentStatus_NonPaidInvoice_ShouldThrowException() {
        // Try reverting DRAFT
        assertThatThrownBy(() -> service.revertPaymentStatus(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PAID invoices can be reverted");

        // Try reverting SENT
        assertThatThrownBy(() -> service.revertPaymentStatus(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PAID invoices can be reverted");
    }

    // Add test for revertPaymentStatus on non-existent invoice

    // --- GET RECENT INVOICES Test ---
    // Add test similar to previous example, verifying arguments and result mapping

}