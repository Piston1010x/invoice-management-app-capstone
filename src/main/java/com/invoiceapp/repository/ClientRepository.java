package com.invoiceapp.repository;

import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository   extends JpaRepository<Client, Long> {
    Optional<User> findByEmail(String email);

}
