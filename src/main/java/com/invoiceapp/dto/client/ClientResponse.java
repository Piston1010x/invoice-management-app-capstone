package com.invoiceapp.dto.client;

//client response dto
public record ClientResponse(
        Long id,
        String name,
        String email,
        String phone
) {}
