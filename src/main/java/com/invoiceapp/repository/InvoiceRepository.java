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
import java.time.LocalDate;
import java.util.Collection;
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
    List<Invoice> findByStatusAndUserAndArchivedFalse(InvoiceStatus status, User user);
    Page<Invoice> findByStatusAndUserAndArchivedFalse(InvoiceStatus status, User user, Pageable pageable);
    Page<Invoice> findByUserAndArchivedFalse(User user, Pageable pageable);
    List<Invoice> findByUser(User user);
    long countByStatusAndUser(InvoiceStatus status, User user);
    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.status = :status AND i.user = :user AND i.archived = false")
    BigDecimal sumTotalByStatusAndUser(@Param("status") InvoiceStatus status, @Param("user") User user);
    long countByUser(User user);
    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.user = :user AND i.archived = false")
    BigDecimal grandTotalByUser(@Param("user") User user);
    long countByStatusAndUserAndArchivedFalse(InvoiceStatus status, User user);

    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.status = :status AND i.user = :user AND i.archived = false")
    BigDecimal sumTotalByStatusAndUserAndArchivedFalse(@Param("status") InvoiceStatus status, @Param("user") User user);


    @Query("""
       select i 
         from Invoice i 
        where i.archived = false 
          and i.status   = 'OVERDUE' 
          and i.dueDate <= :today
       """)
    List<Invoice> findSentAndDueOnOrBefore(
            @Param("status") InvoiceStatus status,
            @Param("today") LocalDate today);
    Page<Invoice> findByUserEmail(String username, Pageable pageable);
    // New: same, but constrained to issueDate between from→to
    long countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
            InvoiceStatus status, User user, LocalDate from, LocalDate to
    );

    @Query("""
      select coalesce(sum(i.total),0) 
      from Invoice i 
      where i.status = :status 
        and i.user = :user 
        and i.archived = false
        and i.issueDate between :from and :to
    """)
    BigDecimal sumTotalByStatusAndUserAndArchivedFalseAndIssueDateBetween(
            @Param("status") InvoiceStatus status,
            @Param("user")   User user,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to
    );

    long countByClientIdAndStatusNot(Long clientId, InvoiceStatus status);

    long countByClientIdAndStatusInAndArchivedFalse(
            Long clientId,
            Collection<InvoiceStatus> statuses);
    long countByClientId(Long clientId);
    void deleteAllByClientId(Long clientId);

}


