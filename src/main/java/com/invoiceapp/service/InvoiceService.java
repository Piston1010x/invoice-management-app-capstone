package com.invoiceapp.service;

import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.dto.invoice.InvoiceRequest;
import com.invoiceapp.dto.invoice.InvoiceResponse;
import com.invoiceapp.dto.invoice.RecordPaymentForm;
import com.invoiceapp.entity.*;
import com.invoiceapp.repository.*;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.util.ClientMapper;
import com.invoiceapp.util.InvoiceMapper;
import com.invoiceapp.util.InvoiceNumberGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final ClientRepository clientRepo;
    private final InvoiceMetricRepository metricRepo;
    private final InvoiceNumberGenerator numberGenerator;
    private final InvoicePdfService pdfService;
    private final EmailService emailService;
    private final UserProvider userProvider;
    private final InvoiceMapper invoiceMapper;
    private final UserRepository userRepository;

    //Create new invoice
    public InvoiceResponse create(InvoiceRequest dto) {
        User user = userProvider.getCurrentUser();
        log.info("User {} is creating a new invoice for client ID: {}", user.getEmail(), dto.clientId());
        Client client = clientRepo.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        Invoice inv = InvoiceMapper.toEntity(dto, client);
        inv.setUser(user);
        log.info("Invoice created with number: {}", inv.getInvoiceNumber());

        return InvoiceMapper.toDto(invoiceRepo.save(inv));
    }



    //list all Invoices
    public Page<InvoiceResponse> list(Optional<InvoiceStatus> status, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("id").descending());
        User usr = userProvider.getCurrentUser();
        log.info("Listing invoices for user {} with status: {} (page: {}, size: {})", usr.getEmail(),
                status.orElse(null), page, size);

        Page<Invoice> src = status
                .map(st -> invoiceRepo.findByStatusAndUserAndArchivedFalse(st, usr, p))
                .orElseGet(() -> invoiceRepo.findByUserAndArchivedFalse(usr, p));

        log.info("Fetched {} invoices for user {}", src.getTotalElements(), usr.getEmail());

        return src.map(InvoiceMapper::toDto);
    }


    /** New overload: admin can pass userId, users only see their own */
    public Page<InvoiceResponse> list(Optional<InvoiceStatus> status,
                                      Optional<Long> userId,
                                      int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("id").descending());
        User me = userProvider.getCurrentUser();

        User target = userId
                .filter(id -> me.getRole() == Role.ADMIN)
                .flatMap(userRepository::findById)
                .orElse(me);

        Page<Invoice> invoices = status
                .map(st -> invoiceRepo.findByStatusAndUserAndArchivedFalse(st, target, p))
                .orElseGet(() -> invoiceRepo.findByUserAndArchivedFalse(target, p));

        return invoices.map(InvoiceMapper::toDto);
    }





    //retrieves entity converts to dto and returns dto
    public InvoiceResponse get(Long id) {
        return InvoiceMapper.toDto(getEntity(id));
    }
    //retrieves raw invoice
    public Invoice getEntity(Long id) {
        log.info("Fetching invoice with ID: {}", id);
        return invoiceRepo.findById(id)
                .orElseThrow(() -> {
                    log.error("Invoice with ID {} not found", id);
                    return new EntityNotFoundException("Invoice not found");
                });
    }






    /** Sends the invoice by email and transitions it from DRAFT to SENT.
     *
     * - Validates that the invoice is in DRAFT status.
     * - Updates status to SENT, sets issue date, generates invoice number and payment token.
     * - Generates a PDF and sends it via email to the client with a payment confirmation link.
     * - Returns the updated InvoiceResponse DTO.
     *
     * @param id the ID of the invoice to send
     * @return the updated InvoiceResponse after sending
     * @throws IllegalStateException if the invoice is not in DRAFT status
     * @throws EntityNotFoundException if the invoice is not found */
    public InvoiceResponse send(Long id) {
        Invoice inv = getEntity(id);
        if (inv.getStatus() != InvoiceStatus.DRAFT) {
            log.error("Attempted to send an invoice that is not in DRAFT status (Invoice ID: {})", id);
            throw new IllegalStateException("Only DRAFT can be sent");
        }
        log.info("Transitioning invoice {} from DRAFT to SENT", inv.getInvoiceNumber());
        inv.setStatus(InvoiceStatus.SENT);
        inv.setIssueDate(LocalDate.now());
        inv.setInvoiceNumber(numberGenerator.nextForUser(inv.getUser()));
        log.info("Generated payment token for invoice {}: {}", inv.getInvoiceNumber(), inv.getPaymentToken());
        inv.setPaymentToken(UUID.randomUUID().toString());



        log.info("Generated invoice PDF for invoice {}", inv.getInvoiceNumber());
        byte[] pdf = pdfService.generate(inv);
        String link = "http://localhost:8080/public/confirm-payment/" + inv.getPaymentToken();
        log.info("Sending invoice {} to client {}", inv.getInvoiceNumber(), inv.getClient().getEmail());
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
        log.info("Invoice {} has been sent to client {}", inv.getInvoiceNumber(), inv.getClient().getEmail());
        return InvoiceMapper.toDto(inv);
    }

    //Method to mark an invoice as PAID
    public InvoiceResponse markPaid(Long id, RecordPaymentForm f) {
        Invoice inv = getEntity(id);
        if (!Set.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE).contains(inv.getStatus())) {
            log.error("Attempted to mark invoice {} as paid when it is not in SENT/OVERDUE status", id);
            throw new IllegalStateException("Only SENT/OVERDUE can be paid");
        }

        inv.setStatus(InvoiceStatus.PAID);
        inv.setPaymentDate(f.getPaymentDate());

        if (f.getPaymentDate().isBefore(inv.getIssueDate())) {
            log.error("Attempted to record a payment with a date before the invoice issue date (Invoice ID: {})", id);
            throw new IllegalArgumentException("Payment date cannot be before the invoice issue date.");
        }

        inv.setPaymentMethod(f.getPaymentMethod());
        inv.setPaymentNotes(f.getPaymentNotes());
        inv.setTransactionId(f.getTransactionId());

        snapshot(inv);
        log.info("Invoice {} marked as PAID. Payment details: Date: {}, Method: {}, Transaction ID: {}",
                inv.getInvoiceNumber(), f.getPaymentDate(), f.getPaymentMethod(), f.getTransactionId());

        return InvoiceMapper.toDto(inv);
    }



    //checks for overdue invoices
    public int markOverdue() {
        int overdueCount = invoiceRepo.findActive(InvoiceStatus.SENT).stream()
                .filter(i -> i.getDueDate().isBefore(LocalDate.now()))
                .peek(i -> {
                    i.setStatus(InvoiceStatus.OVERDUE);
                    snapshot(i);
                })
                .mapToInt(x -> 1)
                .sum();

        log.info("Marked {} invoices as OVERDUE", overdueCount);
        return overdueCount;
    }


    //update invoice if its in draft state
    public InvoiceResponse update(Long invoiceId, InvoiceRequest dto) {
        log.info("Fetching invoice to update " + invoiceId);
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
        log.info("Updating invoice {}. Client: {}, Due Date: {}", inv.getInvoiceNumber(), inv.getClient().getName(), inv.getDueDate());
        for (var r : dto.items()) {
            log.info("Updating item: {}", r.description());
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

        // refreeze user
        inv.setUser(userProvider.getCurrentUser());

        // save & return fresh DTO
        invoiceRepo.save(inv);
        return InvoiceMapper.toDto(inv);
    }

    //soft delete
    public void archive(Long id) {
        Invoice inv = getEntity(id);
        inv.setArchived(true);
        log.info("Invoice {} archived", inv.getInvoiceNumber());
    }

    public void delete(Long id) {
        archive(id);
        log.info("Invoice {} deleted (soft delete)", id);
    }


    //metric snapshot for stats
    public void snapshot(Invoice inv) {
        log.info("taking snapshot for dashboard stats");
        metricRepo.save(new InvoiceMetric(LocalDate.now(), inv.getStatus(), inv.getTotal()));
    }


    //revert a PAID invoice back to SENT, clearing recorded payment details./
    public InvoiceResponse revertPaymentStatus(Long id) {
        Invoice inv = getEntity(id);
        if (inv.getStatus() != InvoiceStatus.PAID) {
            log.error("Only PAID invoices can be reverted (Invoice ID: {})", id);
            throw new IllegalStateException("Only PAID invoices can be reverted");
        }

        inv.setStatus(InvoiceStatus.SENT);
        inv.setPaymentDate(null);
        inv.setPaymentMethod(null);
        inv.setPaymentAmountRecorded(null);
        inv.setPaymentNotes(null);
        inv.setTransactionId(null);

        invoiceRepo.save(inv);
        log.info("Invoice {} reverted to SENT status", inv.getInvoiceNumber());

        return InvoiceMapper.toDto(inv);
    }



    //method for dashboard to retrieve last 5 invoices
    public List<InvoiceResponse> getRecentInvoices(String username, int count) {
        Pageable topFive = PageRequest.of(0, count, Sort.by("issueDate").descending());

        log.info("Fetching top {} recent invoices for user {}", count, username);
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


    public Page<InvoiceResponse> listForUser(
            User user,
            Optional<InvoiceStatus> status,
            int page, int size
    ) {
        Pageable p = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Invoice> src = status
                .map(st -> invoiceRepo.findByStatusAndUserAndArchivedFalse(st, user, p))
                .orElseGet(() -> invoiceRepo.findByUserAndArchivedFalse(user, p));
        return src.map(InvoiceMapper::toDto);
    }
    /**
     * Admin/session-based listing:
     * if user is ADMIN, uses the session-picked user
     * else uses the current principal
     */
    @Transactional(readOnly = true)
    public Page<ClientResponse> listForUser(User user, int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("id").descending());
        return clientRepo.findAllByUser(user, pg).map(ClientMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ClientResponse findByIdForUser(Long id, User user) {
        return clientRepo.findById(id)
                .filter(c -> c.getUser().equals(user))
                .map(ClientMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Client not found or access denied"));
    }


    /**
     * Create a draft invoice *for* the given target user.
     */
    public InvoiceResponse createForUser(InvoiceRequest dto, User target) {
        // look up client
        Client client = clientRepo.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        // map DTO → entity
        Invoice inv = InvoiceMapper.toEntity(dto, client);
        // assign the impersonated user as owner
        inv.setUser(target);
        // save & return
        log.info("User {} is creating an invoice for target user {}. Client ID: {}", userProvider.getCurrentUser().getEmail(), target.getEmail(), dto.clientId());
        return InvoiceMapper.toDto(invoiceRepo.save(inv));
    }

    //Update a DRAFT invoice for the given target user.
    public InvoiceResponse updateForUser(Long invoiceId, InvoiceRequest dto, User target) {
        Invoice inv = getEntity(invoiceId);
        if (inv.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT can be edited");
        }

        //header fields
        log.info("User {} is updating invoice {} for target user {}", userProvider.getCurrentUser().getEmail(), invoiceId, target.getEmail());
        Client client = clientRepo.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        inv.setClient(client);
        inv.setDueDate(dto.dueDate());
        inv.setCurrency(dto.currency());
        inv.setToName(dto.toName());
        inv.setFromName(dto.fromName());
        inv.setBankName(dto.bankName());
        inv.setIban(dto.iban());

        //remove any that the form no longer has —
        Iterator<InvoiceItem> it = inv.getItems().iterator();
        while (it.hasNext()) {
            InvoiceItem existing = it.next();
            boolean stillPresent = dto.items().stream().anyMatch(r ->
                    r.description().equals(existing.getDescription())
                            && r.quantity() == existing.getQuantity()
                            && r.unitPrice().equals(existing.getUnitPrice())
            );
            if (!stillPresent) {
                it.remove();
            }
        }

        //add or update each update
        for (var r : dto.items()) {
            InvoiceItem match = inv.getItems().stream()
                    .filter(i ->
                            i.getDescription().equals(r.description()) &&
                                    i.getQuantity() == r.quantity()
                    ).findFirst()
                    .orElse(null);

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

        // reassign owner
        inv.setUser(target);

        //save and return dto
        invoiceRepo.save(inv);
        return InvoiceMapper.toDto(inv);
    }
}
