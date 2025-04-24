// src/main/java/com/invoiceapp/dto/RegisterForm.java
package com.invoiceapp.dto;

import jakarta.validation.constraints.*;

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
