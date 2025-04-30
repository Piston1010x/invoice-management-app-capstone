package com.invoiceapp.dto.client;

import jakarta.validation.constraints.*;

//client request dto

public record ClientRequest(
        @NotBlank @Size(max = 150)
        String name,
        @Email
        String email,
        @Size(max = 20)
        String phone
) {}
