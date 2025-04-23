package com.invoiceapp.service;

import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.repository.ClientRepository;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.util.InvoiceMapper;
import com.invoiceapp.util.InvoiceNumberGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final InvoiceRepository invoiceRepo;
    private final ClientRepository clientRepo;
    private final InvoiceMapper mapper;
    private final InvoiceNumberGenerator numberGenerator;
    private final InvoicePdfService pdfService;
    private final EmailService emailService;

    public InvoiceResponse create(InvoiceRequest dto) {
        Client client = clientRepo.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client %d".formatted(dto.clientId())));

        Invoice saved = invoiceRepo.save(InvoiceMapper.toEntity(dto, client));
        return InvoiceMapper.toDto(saved);
    }

    @Transactional
    public List<InvoiceResponse> list(Optional<InvoiceStatus> status) {

        List<Invoice> invoices = status
                .map(invoiceRepo::findByStatus)   // when filter present
                .orElseGet(invoiceRepo::findAll); // otherwise everything

        return invoices.stream()
                .map(InvoiceMapper::toDto)
                .toList();
    }

    public InvoiceResponse get(Long id) {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        return InvoiceMapper.toDto(invoice);
    }


    public InvoiceResponse send(Long id) {
        Invoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice " + id + " not found"));

        if (inv.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be sent");
        }

        inv.setStatus(InvoiceStatus.SENT);
        inv.setIssueDate(LocalDate.now());
        inv.setInvoiceNumber(numberGenerator.next());

        inv.setPaymentToken(UUID.randomUUID().toString());

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



        return mapper.toDto(inv);
    }

    public InvoiceResponse markPaid(Long id) {
        Invoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice " + id + " not found"));

        if (!Set.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE).contains(inv.getStatus())) {
            throw new IllegalStateException("Only SENT or OVERDUE invoices can be marked paid");
        }

        inv.setStatus(InvoiceStatus.PAID);
        return mapper.toDto(inv);
    }
    @Transactional
    public Invoice getEntity(Long id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
    }
}
