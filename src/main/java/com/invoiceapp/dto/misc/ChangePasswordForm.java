// src/main/java/com/invoiceapp/dto/ChangePasswordForm.java
package com.invoiceapp.dto.misc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

//Change pass form for users with user role
@Data
public class ChangePasswordForm {

    @NotBlank(message = "Old password cannot be empty")
    private String oldPassword;

    @NotBlank(message = "New password cannot be empty")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Password confirmation cannot be empty")
    private String confirmPassword;

}