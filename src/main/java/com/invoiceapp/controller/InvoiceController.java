package com.invoiceapp.controller;

import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.util.InvoiceMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<InvoiceResponse> all(@RequestParam Optional<InvoiceStatus> status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/send")
    public InvoiceResponse send(@PathVariable Long id) {
        return service.send(id);
    }

    @PostMapping("/{id}/mark-paid")
    public InvoiceResponse markPaid(@PathVariable Long id) {
        return service.markPaid(id);
    }
}
