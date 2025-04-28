package com.invoiceapp;

import com.invoiceapp.dto.InvoiceItemRequest;
import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.dto.RecordPaymentForm;
import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceMetric;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.ClientRepository;
import com.invoiceapp.repository.InvoiceMetricRepository;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.util.InvoiceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository      invoiceRepo;
    @Mock ClientRepository       clientRepo;
    @Mock InvoiceMetricRepository metricRepo;
    @Mock com.invoiceapp.util.InvoiceNumberGenerator numberGenerator;
    @Mock com.invoiceapp.service.InvoicePdfService      pdfService;
    @Mock com.invoiceapp.service.EmailService emailService;
    @Mock UserProvider userProvider;
    @Mock InvoiceMapper invoiceMapper; // injected but unused—mapping is static

    @InjectMocks
    InvoiceService service;

    private User   fakeUser;
    private Client fakeClient;

    @BeforeEach
    void setUp() {
        // 1) Fake user + lenient stub
        fakeUser = new User();
        fakeUser.setId(42L);
        // make it lenient so tests that don't call getCurrentUser() don't error
        lenient().when(userProvider.getCurrentUser()).thenReturn(fakeUser);

        // 2) Fake client
        fakeClient = new Client();
        fakeClient.setId(99L);
        fakeClient.setName("Acme Ltd");
        fakeClient.setEmail("pay@acme.com");
    }

    @Test
    void create_happyPath_savesAndReturnsDto() {
        // arrange
        InvoiceRequest dto = new InvoiceRequest(
                99L,
                List.<InvoiceItemRequest>of(),
                LocalDate.now().plusDays(30),
                com.invoiceapp.entity.Currency.USD,
                "ClientName", "MyName", "Bank", "IBAN123"
        );
        when(clientRepo.findById(99L)).thenReturn(Optional.of(fakeClient));
        ArgumentCaptor<Invoice> capt = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepo.save(capt.capture()))
                .thenAnswer(inv -> {
                    Invoice i = inv.getArgument(0);
                    i.setId(555L);
                    return i;
                });

        // act
        InvoiceResponse resp = service.create(dto);

        // assert
        Invoice saved = capt.getValue();
        assertThat(saved.getUser()).isSameAs(fakeUser);
        assertThat(saved.getClient()).isSameAs(fakeClient);
        assertThat(resp.id()).isEqualTo(555L);
    }

    @Test
    void list_whenNoStatus_invokesFindByUser() {
        // arrange
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Invoice inv = new Invoice();
        inv.setClient(fakeClient); // avoid NPE in mapper
        Page<Invoice> pg = new PageImpl<>(List.of(inv), pageable, 1);
        when(invoiceRepo.findByUserAndArchivedFalse(eq(fakeUser), any(Pageable.class)))
                .thenReturn(pg);

        // act
        Page<InvoiceResponse> result = service.list(Optional.empty(), page, size);

        // assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(invoiceRepo).findByUserAndArchivedFalse(fakeUser, pageable);
    }

    @Test
    void send_fromDraft_sendsEmailAndSnapshotsMetric() {
        // arrange
        Invoice inv = new Invoice();
        inv.setStatus(InvoiceStatus.DRAFT);
        inv.setClient(fakeClient);     // avoid NPE
        inv.setItems(List.of());
        when(invoiceRepo.findById(77L)).thenReturn(Optional.of(inv));
        when(numberGenerator.next()).thenReturn("INV1000");
        when(pdfService.generate(inv)).thenReturn(new byte[]{0x1,0x2});

        // act
        service.send(77L);

        // assert
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(inv.getInvoiceNumber()).isEqualTo("INV1000");
        assertThat(inv.getIssueDate()).isEqualTo(LocalDate.now());
        assertThat(inv.getPaymentToken()).isNotNull();

        verify(emailService).sendInvoice(
                eq(fakeClient.getEmail()),
                contains("INV1000"),
                anyString(),
                any(byte[].class),
                eq("INV1000.pdf")
        );
        verify(metricRepo).save(any(InvoiceMetric.class));
    }

    @Test
    void markOverdue_transitionsPastDue() {
        // arrange
        Invoice a = new Invoice();
        a.setStatus(InvoiceStatus.SENT);
        a.setDueDate(LocalDate.now().minusDays(1));
        a.setClient(fakeClient);

        Invoice b = new Invoice();
        b.setStatus(InvoiceStatus.SENT);
        b.setDueDate(LocalDate.now().minusDays(2));
        b.setClient(fakeClient);

        when(invoiceRepo.findActive(InvoiceStatus.SENT)).thenReturn(List.of(a,b));

        // act
        int changed = service.markOverdue();

        // assert
        assertThat(changed).isEqualTo(2);
        assertThat(a.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        assertThat(b.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        verify(metricRepo, times(2)).save(any(InvoiceMetric.class));
    }

    @Test
    void revertPaymentStatus_fromPaid_revertsAndClears() {
        // arrange
        Invoice inv = new Invoice();
        inv.setId(99L);
        inv.setStatus(InvoiceStatus.PAID);
        inv.setClient(fakeClient);  // avoid NPE
        when(invoiceRepo.findById(99L)).thenReturn(Optional.of(inv));

        // act
        InvoiceResponse resp = service.revertPaymentStatus(99L);

        // assert
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(resp.status()).isEqualTo(InvoiceStatus.SENT);
        verify(invoiceRepo).save(inv);
    }

    @Test
    void markPaid_invalidState_throws() {
        // arrange
        Invoice inv = new Invoice();
        inv.setStatus(InvoiceStatus.DRAFT);
        when(invoiceRepo.findById(5L)).thenReturn(Optional.of(inv));

        RecordPaymentForm form = new RecordPaymentForm();
        form.setPaymentDate(LocalDate.now());
        form.setPaymentMethod("Cash");

        // act & assert
        assertThrows(IllegalStateException.class,
                () -> service.markPaid(5L, form));
    }

    @Test
    void getRecentInvoices_returnsTopN() {
        // arrange
        Invoice i1 = new Invoice();
        i1.setId(1L);
        i1.setInvoiceNumber("A");
        i1.setClient(fakeClient);  // avoid NPE

        Invoice i2 = new Invoice();
        i2.setId(2L);
        i2.setInvoiceNumber("B");
        i2.setClient(fakeClient);

        Page<Invoice> pg = new PageImpl<>(List.of(i1,i2));
        when(invoiceRepo.findByUserEmail(
                eq("user@example.com"),
                eq(PageRequest.of(0, 2, Sort.by("issueDate").descending()))
        )).thenReturn(pg);

        // act
        var list = service.getRecentInvoices("user@example.com", 2);

        // assert
        assertThat(list).extracting(InvoiceResponse::id).containsExactly(1L, 2L);
    }
}
