package com.invoiceapp.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.NumberFormat;


//client form
@Data
public class ClientForm {
    private Long id; // ← for editing

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid e-mail")
    private String email;

    @NotBlank(message = "Country code is required")
    private String countryCode;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]+$", message = "Phone number must contain only digits.")
    private String rawPhone;

    /** Combined for persistence */
    private String phone;
}
