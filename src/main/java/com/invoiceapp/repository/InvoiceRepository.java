package com.invoiceapp.repository;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository  extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStatus(InvoiceStatus status);
    Optional<Invoice> findByPaymentToken(String token);
    long count();
    long countByStatus(InvoiceStatus status);

    @Query("select coalesce(sum(i.total),0) from Invoice i where i.status = :status")
    BigDecimal sumByStatus(@Param("status") InvoiceStatus status);

    @Query("select coalesce(sum(i.total),0) from Invoice i")
    BigDecimal grandTotal();


    // InvoiceRepository.java
    @Query("""
           select coalesce(sum(it.unitPrice * it.quantity), 0)
           from InvoiceItem it
           where it.invoice.status = :status
           """)
    BigDecimal sumAmountByStatus(@Param("status") InvoiceStatus status);

    @Query("""
           select coalesce(sum(it.unitPrice * it.quantity), 0)
           from InvoiceItem it
           """)
    BigDecimal grandAmount();

    // InvoiceRepository.java
    @Query("select i from Invoice i where i.archived = false and i.status = :status")
    List<Invoice> findActive(@Param("status") InvoiceStatus status);

    @Query("select i from Invoice i where i.archived = false")
    List<Invoice> findAllActive();


    // InvoiceRepository
    Page<Invoice> findByStatusAndArchivedFalse(InvoiceStatus status, Pageable p);
    Page<Invoice> findByArchivedFalse(Pageable p);


}

