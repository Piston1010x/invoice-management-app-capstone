package com.invoiceapp.repository;

import com.invoiceapp.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
//repo class for user entity
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(@NotBlank @Email String email);
    boolean existsByEmail(String email);   // ⇦ we’ll use this for duplicate check
}
