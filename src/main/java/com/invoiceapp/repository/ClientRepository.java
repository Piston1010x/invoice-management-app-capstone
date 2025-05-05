package com.invoiceapp.repository;

import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Page<Client> findAll(Pageable pageable);
    Page<Client> findAllByUser(User user, Pageable pageable);

    //Methods for checking duplicates during new client creation
    boolean existsByNameAndUser(String name, User user);
    boolean existsByEmailAndUser(String email, User user);
    boolean existsByPhoneAndUser(String phone, User user); // <-- Add this

        //check for duplicates during update/edit of client
        @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Client c WHERE LOWER(c.name) = LOWER(:name) AND c.user = :user AND c.id != :id")
        boolean existsByNameAndUserAndIdNot(@Param("name") String name, @Param("user") User user, @Param("id") Long id);

        // Case-insensitive check for email duplicates
        @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Client c WHERE LOWER(c.email) = LOWER(:email) AND c.user = :user AND c.id != :id")
        boolean existsByEmailAndUserAndIdNot(@Param("email") String email, @Param("user") User user, @Param("id") Long id);

        // Case-insensitive check for phone duplicates
        @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Client c WHERE LOWER(c.phone) = LOWER(:phone) AND c.user = :user AND c.id != :id")
        boolean existsByPhoneAndUserAndIdNot(@Param("phone") String phone, @Param("user") User user, @Param("id") Long id);
    }


