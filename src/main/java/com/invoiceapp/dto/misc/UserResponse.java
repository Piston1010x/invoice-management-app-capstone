package com.invoiceapp.dto.misc;

import com.invoiceapp.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

//view of a user for admin screens.
@Data
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private Role role;
}
