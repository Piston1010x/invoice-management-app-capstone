package com.invoiceapp.dto;

public record ClientResponse(
        Long id,
        String name,
        String email,
        String phone
) {}
