package com.invoiceapp.dto.misc;

import jakarta.validation.constraints.*;

//registration form
public record RegisterForm(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 chars")
        String password,

        @NotBlank
        String passwordConfirm
) { }
