package com.invoiceapp.repository;

import com.invoiceapp.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
//repo class for user entity
public interface UserRepository extends JpaRepository<User, Long> {

    //Finds a user by their email address.
    Optional<User> findByEmail(@NotBlank @Email String email);

    //Checks if a user with the given email address already exists.
    boolean existsByEmail(String email);   // ⇦ we’ll use this for duplicate check
}
