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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository       invoiceRepo;
    private final ClientRepository        clientRepo;
    private final InvoiceMetricRepository metricRepo;
    private final InvoiceNumberGenerator numberGenerator;
    private final InvoicePdfService       pdfService;
    private final EmailService            emailService;
    private final UserProvider            userProvider;
    private final InvoiceMapper           invoiceMapper;

    // ─── CREATE ─────────────────────────────────────────────────────────
    public InvoiceResponse create(InvoiceRequest dto) {
        User user = userProvider.getCurrentUser();
        Client client = clientRepo.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        Invoice inv = InvoiceMapper.toEntity(dto, client);
        inv.setUser(user);
        return InvoiceMapper.toDto(invoiceRepo.save(inv));
    }

    // ─── LIST ────────────────────────────────────────────────────────────
    public Page<InvoiceResponse> list(Optional<InvoiceStatus> status, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("id").descending());
        User usr = userProvider.getCurrentUser();
        Page<Invoice> src = status
                .map(st -> invoiceRepo.findByStatusAndUserAndArchivedFalse(st, usr, p))
                .orElseGet(() -> invoiceRepo.findByUserAndArchivedFalse(usr, p));
        return src.map(InvoiceMapper::toDto);
    }

    // ─── READ ────────────────────────────────────────────────────────────
    public InvoiceResponse get(Long id) {
        return InvoiceMapper.toDto(getEntity(id));
    }
    public Invoice getEntity(Long id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    }

    // ─── SEND ────────────────────────────────────────────────────────────
    public InvoiceResponse send(Long id) {
        Invoice inv = getEntity(id);
        if (inv.getStatus() != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT can be sent");
        inv.setStatus(InvoiceStatus.SENT);
        inv.setIssueDate(LocalDate.now());
        inv.setInvoiceNumber(numberGenerator.next());
        inv.setPaymentToken(UUID.randomUUID().toString());

        byte[] pdf = pdfService.generate(inv);
        String link = "http://localhost:8080/public/confirm-payment/" + inv.getPaymentToken();
        emailService.sendInvoice(
                inv.getClient().getEmail(),
                "Invoice " + inv.getInvoiceNumber(),
                "Dear " + inv.getClient().getName() + ",<br><br>" +
                        "Please find your invoice attached.<br>" +
                        "When you have paid, click <a href=\"" + link + "\">this link</a>.",
                pdf,
                inv.getInvoiceNumber() + ".pdf"
        );

        snapshot(inv);
        return InvoiceMapper.toDto(inv);
    }

    // ─── MARK PAID ────────────────────────────────────────────────────────
    public InvoiceResponse markPaid(Long id, RecordPaymentForm f) {
        Invoice inv = getEntity(id);
        if (!Set.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE).contains(inv.getStatus()))
            throw new IllegalStateException("Only SENT/OVERDUE can be paid");
        inv.setStatus(InvoiceStatus.PAID);
        inv.setPaymentDate(f.getPaymentDate());
        inv.setPaymentMethod(f.getPaymentMethod());
        inv.setPaymentNotes(f.getPaymentNotes());
        snapshot(inv);
        return InvoiceMapper.toDto(inv);
    }

    public InvoiceResponse markPaid(Long id,
                                    RecordPaymentForm f,
                                    BigDecimal amount) {
        Invoice inv = getEntity(id);
        if (!Set.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE).contains(inv.getStatus())) {
            throw new IllegalStateException("Only SENT/OVERDUE can be paid");
        }
        inv.setStatus(InvoiceStatus.PAID);
        inv.setPaymentDate(f.getPaymentDate());
        inv.setPaymentMethod(f.getPaymentMethod());
        inv.setPaymentAmountRecorded(amount);
        inv.setPaymentNotes(f.getPaymentNotes());
        snapshot(inv);
        return InvoiceMapper.toDto(inv);
    }

    // ─── OVERDUE SWEEP ───────────────────────────────────────────────────
    public int markOverdue() {
        return invoiceRepo.findActive(InvoiceStatus.SENT).stream()
                .filter(i -> i.getDueDate().isBefore(LocalDate.now()))
                .peek(i -> { i.setStatus(InvoiceStatus.OVERDUE); snapshot(i); })
                .mapToInt(x -> 1).sum();
    }

    // ─── UPDATE (EDIT DRAFT) ─────────────────────────────────────────────
    public InvoiceResponse update(Long invoiceId, InvoiceRequest dto) {
        Invoice inv = getEntity(invoiceId);
        if (inv.getStatus() != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT can be edited");

        // update header fields
        Client client = clientRepo.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        inv.setClient(client);
        inv.setDueDate(dto.dueDate());
        inv.setCurrency(dto.currency());
        inv.setToName(dto.toName());
        inv.setFromName(dto.fromName());
        inv.setBankName(dto.bankName());
        inv.setIban(dto.iban());

        // reconcile items
        Iterator<InvoiceItem> it = inv.getItems().iterator();
        while (it.hasNext()) {
            InvoiceItem existing = it.next();
            boolean stillExists = dto.items().stream().anyMatch(r ->
                    r.description().equals(existing.getDescription()) &&
                            existing.getQuantity().equals(r.quantity()) &&
                            r.unitPrice().equals(existing.getUnitPrice())
            );
            if (!stillExists) it.remove();
        }
        // add/update
        for (var r : dto.items()) {
            InvoiceItem match = inv.getItems().stream()
                    .filter(i ->
                            i.getDescription().equals(r.description()) &&
                                    i.getQuantity().equals(r.quantity())
                    )
                    .findFirst().orElse(null);

            if (match == null) {
                InvoiceItem ni = new InvoiceItem();
                ni.setInvoice(inv);
                ni.setDescription(r.description());
                ni.setQuantity(r.quantity());
                ni.setUnitPrice(r.unitPrice());
                inv.getItems().add(ni);
            } else {
                match.setQuantity(r.quantity());
                match.setUnitPrice(r.unitPrice());
            }
        }

        // re-freeze user
        inv.setUser(userProvider.getCurrentUser());

        // save & return fresh DTO
        invoiceRepo.save(inv);
        return InvoiceMapper.toDto(inv);
    }

    // ─── SOFT DELETE ─────────────────────────────────────────────────────
    public void archive(Long id) {
        getEntity(id).setArchived(true);
    }
    public void delete(Long id) {
        archive(id);
    }

    // ─── METRIC SNAPSHOT ─────────────────────────────────────────────────
    private void snapshot(Invoice inv) {
        metricRepo.save(new InvoiceMetric(LocalDate.now(), inv.getStatus(), inv.getTotal()));
    }
    /**
     * Revert a PAID invoice back to SENT, clearing recorded payment details.
     */
    public InvoiceResponse revertPaymentStatus(Long id) {
        Invoice inv = getEntity(id);
        if (inv.getStatus() != InvoiceStatus.PAID) {
            throw new IllegalStateException("Only PAID invoices can be reverted");
        }

        // revert status and clear payment fields
        inv.setStatus(InvoiceStatus.SENT);
        inv.setPaymentDate(null);
        inv.setPaymentMethod(null);
        inv.setPaymentAmountRecorded(null);
        inv.setPaymentNotes(null);

        // persist changes
        invoiceRepo.save(inv);

        // return the updated DTO
        return InvoiceMapper.toDto(inv);
    }

    public List<InvoiceResponse> getRecentInvoices(String username, int count) {
        Pageable topFive = PageRequest.of(0, count, Sort.by("issueDate").descending());
        return invoiceRepo.findByUserEmail(username, topFive)
                .stream()
                .map(inv -> InvoiceResponse.builder()
                        .id(inv.getId())
                        .invoiceNumber(inv.getInvoiceNumber())
                        .clientName(inv.getClient().getName())
                        .total(inv.getTotal())
                        .status(inv.getStatus())
                        .issueDate(inv.getIssueDate())
                        .dueDate(inv.getDueDate())
                        .build()
                )
                .toList();
    }


}