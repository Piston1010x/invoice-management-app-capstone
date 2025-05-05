package com.invoiceapp.dto.misc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
//user password reset form
public class UserPasswordResetForm {
    @NotBlank(message="Current password is required")
    private String oldPassword;

    @NotBlank @Size(min=6, message="Must be at least 8 characters")
    private String newPassword;

    @NotBlank(message="Please confirm")
    private String confirmPassword;
}
