package com.invoiceapp.dto;

import jakarta.validation.constraints.*;


public record ClientRequest(
        @NotBlank @Size(max = 150) String name,
        @Email String email,
        @Size(max = 20) String phone
) {}
