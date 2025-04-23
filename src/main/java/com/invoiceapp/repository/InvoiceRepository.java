package com.invoiceapp.repository;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository  extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStatus(InvoiceStatus status);
    Optional<Invoice> findByPaymentToken(String token);

}

