package com.invoiceapp.repository;

import com.invoiceapp.entity.Currency;
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
//repo class for invoice entity
public interface InvoiceRepository  extends JpaRepository<Invoice, Long> {

    //Finds an invoice by its payment token
    Optional<Invoice> findByPaymentToken(String token);

    //Counts the total number of invoices.
    long count();

    //Retrieves all active invoices
    @Query("select i from Invoice i where i.archived = false and i.status = :status")
    List<Invoice> findActive(@Param("status") InvoiceStatus status);

    //Retrieves a paginated list of non-archived invoices for a user filtered by status.
    Page<Invoice> findByStatusAndUserAndArchivedFalse(InvoiceStatus status, User user, Pageable pageable);

    //Retrieves a paginated list of non-archived invoices for a specific user.
    Page<Invoice> findByUserAndArchivedFalse(User user, Pageable pageable);


    //Finds all overdue invoices (status OVERDUE) that are due on or before the given date and not
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



    //Finds a paginated list of invoices based on user email.
    Page<Invoice> findByUserEmail(String username, Pageable pageable);


    //Counts invoices by status for a user within a specified issue date range
    long countByStatusAndUserAndArchivedFalseAndIssueDateBetween(
            InvoiceStatus status, User user, LocalDate from, LocalDate to
    );



    //Sums total amounts of invoices by status for a user within a specified issue date range (non-archived).
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

    //Counts non-archived invoices for a client by a set of statuses
    long countByClientIdAndStatusInAndArchivedFalse(
            Long clientId,
            Collection<InvoiceStatus> statuses);

    //Deletes all invoices associated with a given client.
    void deleteAllByClientId(Long clientId);




    //Sums total amounts of invoices by status and currency for a user within a specified issue date range (non-archived).
    @Query("""
      select sum(i.total)
      from Invoice i
      where i.status      = :status
        and i.user        = :user
        and i.currency    = :currency
        and i.archived    = false
        and i.issueDate  between :from and :to
    """)
    BigDecimal sumTotalByStatusAndUserAndCurrencyAndArchivedFalseAndIssueDateBetween(
            @Param("status")   InvoiceStatus status,
            @Param("user")     User        user,
            @Param("currency") Currency    currency,
            @Param("from")     LocalDate   from,
            @Param("to")       LocalDate   to);


    // returns e.g. "INV-00042" or empty if none yet
    // Finds the highest invoice number for a user or creates the first one if empty.
    @Query("""
       select max(i.invoiceNumber)
       from Invoice i
       where i.user = :user""")
    Optional<String> findMaxInvoiceNumberForUser(@Param("user") User user);

}



