package com.invoiceapp.service;

import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.entity.*;
import com.invoiceapp.repository.*;
import com.invoiceapp.util.InvoiceMapper;
import com.invoiceapp.util.InvoiceNumberGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
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
    private final InvoiceMapper           mapper;
    private final InvoiceNumberGenerator  numberGenerator;
    private final InvoicePdfService       pdfService;
    private final EmailService            emailService;
    private final InvoiceMapper invoiceMapper;


    /* ------------------------------------------------------------------
       CREATE
       ------------------------------------------------------------------ */
    public InvoiceResponse create(InvoiceRequest dto) {

        Client client = clientRepo.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client %d not found".formatted(dto.clientId())));

        Invoice saved = invoiceRepo.save(InvoiceMapper.toEntity(dto, client));
        return mapper.toDto(saved);
    }


    /* ------------------------------------------------------------------
       LIST  (only NON-archived invoices are returned)
       ------------------------------------------------------------------ */
    public Page<InvoiceResponse> list(Optional<InvoiceStatus> status,
                                      int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Invoice> src = status
                .map(st -> invoiceRepo.findByStatusAndArchivedFalse(st, p))
                .orElseGet(() -> invoiceRepo.findByArchivedFalse(p));

        return src.map(InvoiceMapper::toDto);   // ✔ compiles
    }



    /* ------------------------------------------------------------------
       READ single DTO / Entity
       ------------------------------------------------------------------ */
    public InvoiceResponse get(Long id) {           // DTO
        return mapper.toDto(getEntity(id));
    }

    public Invoice getEntity(Long id) {             // Entity
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Invoice %d not found".formatted(id)));
    }



    public InvoiceResponse send(Long id) {

        Invoice inv = getEntity(id);

        if (inv.getStatus() != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT invoices can be sent");

        inv.setStatus(InvoiceStatus.SENT);
        inv.setIssueDate(LocalDate.now());
        inv.setInvoiceNumber(numberGenerator.next());
        inv.setPaymentToken(UUID.randomUUID().toString());

        /* === generate & e-mail PDF === */
        byte[] pdf = pdfService.generate(inv);

        String link = "http://localhost:8080/public/confirm-payment/" + inv.getPaymentToken();

        emailService.sendInvoice(
                inv.getClient().getEmail(),
                "Invoice " + inv.getInvoiceNumber(),
                """
                Dear %s,<br><br>
                Please find your invoice attached.<br>
                When you have paid, click <a href="%s">this link</a> to notify us.
                """.formatted(inv.getClient().getName(), link),
                pdf,
                inv.getInvoiceNumber() + ".pdf"
        );

        snapshot(inv);                // <-- NEW metric row
        return mapper.toDto(inv);
    }


    /* ------------------------------------------------------------------
       MARK PAID  — also snapshot
       ------------------------------------------------------------------ */
    public InvoiceResponse markPaid(Long id) {

        Invoice inv = getEntity(id);

        if (!Set.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE).contains(inv.getStatus()))
            throw new IllegalStateException("Only SENT / OVERDUE invoices can be marked paid");

        inv.setStatus(InvoiceStatus.PAID);
        snapshot(inv);                // <-- NEW metric row
        return mapper.toDto(inv);
    }


    /* ------------------------------------------------------------------
       DAILY JOB (or manual)  – mark overdue + snapshot
       ------------------------------------------------------------------ */
    public int markOverdue() {

        return invoiceRepo.findActive(InvoiceStatus.SENT).stream()
                .filter(i -> i.getDueDate().isBefore(LocalDate.now()))
                .peek(i -> {
                    i.setStatus(InvoiceStatus.OVERDUE);
                    snapshot(i);
                })
                .mapToInt(x -> 1).sum();
    }


    /* ------------------------------------------------------------------
       SOFT-DELETE  (archive)
       ------------------------------------------------------------------ */
    public void archive(Long id) {
        Invoice inv = getEntity(id);
        inv.setArchived(true);
    }

    /* You can keep this alias if your UI still posts to /delete */
    public void delete(Long id) { archive(id); }


    /* ------------------------------------------------------------------
       PRIVATE helper : write an InvoiceMetric row
       ------------------------------------------------------------------ */
    private void snapshot(Invoice inv) {
        metricRepo.save(new InvoiceMetric(
                LocalDate.now(),
                inv.getStatus(),
                inv.getTotal()));
    }
}
