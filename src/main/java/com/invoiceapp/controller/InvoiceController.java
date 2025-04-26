package com.invoiceapp.controller;

import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.UserRepository;
import com.invoiceapp.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest req,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }


    // src/main/java/com/invoiceapp/controller/InvoiceController.java
    @GetMapping
    public List<InvoiceResponse> all(@RequestParam Optional<InvoiceStatus> status,
                                     @RequestParam(defaultValue = "0")  int page,
                                     @RequestParam(defaultValue = "20") int size) {

        return service.list(status, page, size)         // <- new signature
                .getContent();                    // return the page content only
    }


    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/send")
    public InvoiceResponse send(@PathVariable Long id) {
        return service.send(id);
    }

   /** @PostMapping("/{id}/mark-paid")
    public InvoiceResponse markPaid(@PathVariable Long id) {
        return service.markPaid(id);
    }**/
}
