// dto/RegistrationForm.java
package com.invoiceapp.dto;

import jakarta.validation.constraints.*;

public record RegistrationForm(
        @NotBlank  @Email    String email,
        @NotBlank  @Size(min = 6, message = "Password ≥ 6 chars") String password,
        @NotBlank  String confirm  // we’ll compare in controller
) {}
