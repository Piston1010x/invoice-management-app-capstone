package com.invoiceapp.controller;

import com.invoiceapp.dto.*;
import com.invoiceapp.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService service;

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest req) {
        ClientResponse resp = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public List<ClientResponse> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ClientResponse one(@PathVariable Long id) {
        return service.findById(id);
    }
}
