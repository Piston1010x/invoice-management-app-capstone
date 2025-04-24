package com.invoiceapp.service;

import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.dto.RecordPaymentForm;
import com.invoiceapp.entity.*;
import com.invoiceapp.repository.*;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.util.InvoiceMapper;
import com.invoiceapp.util.InvoiceNumberGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository       invoiceRepo;
    private final ClientRepository        clientRepo;
    private final InvoiceMetricRepository metricRepo;
    private final InvoiceNumberGenerator  numberGenerator;
    private final InvoicePdfService       pdfService;
    private final EmailService            emailService;
    private final UserProvider            userProvider;
    private final InvoiceMapper invoiceMapper;

    /* ────────────────────  CREATE  ──────────────────── */
    public InvoiceResponse create(InvoiceRequest dto, User user) {

        Client client = clientRepo.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client %d not found".formatted(dto.clientId())));

        Invoice inv = InvoiceMapper.toEntity(dto, client);
        inv.setUser(user);

        return InvoiceMapper.toDto(invoiceRepo.save(inv));
    }


    /* ────────────────────  LIST (non-archived)  ──────────────────── */
    @Transactional
    public Page<InvoiceResponse> list(Optional<InvoiceStatus> status,
                                      int page, int size) {

        Pageable p   = PageRequest.of(page, size, Sort.by("id").descending());
        User     usr = userProvider.getCurrentUser();

        Page<Invoice> src = status
                .map(st -> invoiceRepo.findByStatusAndUserAndArchivedFalse(st, usr, p))
                .orElseGet(() -> invoiceRepo.findByUserAndArchivedFalse(usr, p));

        return src.map(InvoiceMapper::toDto);
    }


    /* ────────────────────  READ  ──────────────────── */
    public InvoiceResponse get(Long id) { return InvoiceMapper.toDto(getEntity(id)); }

    public Invoice getEntity(Long id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice %d not found".formatted(id)));
    }


    /* ────────────────────  SEND  ──────────────────── */
    public InvoiceResponse send(Long id) {

        Invoice inv = getEntity(id);

        if (inv.getStatus() != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT invoices can be sent");

        inv.setStatus(InvoiceStatus.SENT);
        inv.setIssueDate(LocalDate.now());
        inv.setInvoiceNumber(numberGenerator.next());
        inv.setPaymentToken(UUID.randomUUID().toString());

        /* generate & mail PDF */
        byte[] pdf = pdfService.generate(inv);
        String link = "http://localhost:8080/public/confirm-payment/" + inv.getPaymentToken();

        emailService.sendInvoice(inv.getClient().getEmail(),
                "Invoice " + inv.getInvoiceNumber(),
                """
                Dear %s,<br><br>
                Please find your invoice attached.<br>
                When you have paid, click <a href="%s">this link</a> to notify us.
                """.formatted(inv.getClient().getName(), link),
                pdf,
                inv.getInvoiceNumber() + ".pdf");

        snapshot(inv);
        return InvoiceMapper.toDto(inv);
    }


    /* ────────────────────  MARK PAID (with details)  ──────────────────── */
    public InvoiceResponse markPaid(Long id, RecordPaymentForm f) {

        Invoice inv = getEntity(id);

        if (!Set.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE).contains(inv.getStatus()))
            throw new IllegalStateException("Only SENT / OVERDUE invoices can be paid");

        inv.setStatus(InvoiceStatus.PAID);
        inv.setPaymentDate    (f.getPaymentDate());
        inv.setPaymentMethod  (f.getPaymentMethod());
        inv.setPaymentAmountRecorded(f.getPaymentAmount());
        inv.setPaymentNotes   (f.getPaymentNotes());

        snapshot(inv);
        return InvoiceMapper.toDto(inv);
    }


    /* ────────────────────  DAILY OVERDUE JOB  ──────────────────── */
    public int markOverdue() {
        return invoiceRepo.findActive(InvoiceStatus.SENT).stream()
                .filter(i -> i.getDueDate().isBefore(LocalDate.now()))
                .peek(i -> { i.setStatus(InvoiceStatus.OVERDUE); snapshot(i); })
                .mapToInt(x -> 1).sum();
    }


    /* ────────────────────  SOFT-DELETE  ──────────────────── */
    public void archive(Long id) { getEntity(id).setArchived(true); }
    public void delete (Long id) { archive(id); }          // alias


    /* ────────────────────  METRIC SNAPSHOT  ──────────────────── */
    private void snapshot(Invoice inv) {
        metricRepo.save(new InvoiceMetric(
                LocalDate.now(), inv.getStatus(), inv.getTotal()));
    }

    public InvoiceResponse revertPaymentStatus(Long id) {
        Invoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice %d not found".formatted(id)));

        if (inv.getStatus() != InvoiceStatus.PAID) {
            throw new IllegalStateException("Only PAID invoices can be reverted");
        }

        // revert status
        inv.setStatus(InvoiceStatus.SENT);

        // clear out all the recorded payment details
        inv.setPaymentDate(null);
        inv.setPaymentMethod(null);
        inv.setPaymentAmountRecorded(null);
        inv.setPaymentNotes(null);

        // persist
        invoiceRepo.save(inv);

        return invoiceMapper.toDto(inv);
    }
}
