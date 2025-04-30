package com.invoiceapp.repository;

import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
//repo class for client entity
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<User> findByEmail(String email);
    Page<Client> findAll(Pageable pageable);
    List<Client> findAllByUser(User user);
    Page<Client> findAllByUser(User user, Pageable pageable);
    boolean existsByEmailAndUser(String email, User user);
    boolean existsByPhoneAndUser(String phone, User user);

}
