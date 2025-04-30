package com.invoiceapp.repository;

import com.invoiceapp.entity.Currency;
import com.invoiceapp.entity.InvoiceMetric;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

//repo class for invoiceMetric entity
public interface InvoiceMetricRepository extends JpaRepository<InvoiceMetric,Long> {

    interface CurrencySum {
        Currency getCurrency();
        BigDecimal getTotal();
    }

    @Query("""
      SELECT i.currency    AS currency,
             SUM(i.total)   AS total
        FROM Invoice i
       WHERE i.status   = :status
         AND i.user     = :user
         AND i.archived = false
         AND i.issueDate BETWEEN :from AND :to
       GROUP BY i.currency
      """)
    List<CurrencySum> sumTotalByStatusAndUserAndDateGroupedByCurrency(
            @Param("status") InvoiceStatus status,
            @Param("user")   User user,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to
    );

    @Query("""
      SELECT i.currency    AS currency,
             SUM(i.total)   AS total
        FROM Invoice i
       WHERE i.status   IN :statuses
         AND i.user     = :user
         AND i.archived = false
         AND i.issueDate BETWEEN :from AND :to
       GROUP BY i.currency
      """)
    List<CurrencySum> sumTotalByStatusesAndUserAndDateGroupedByCurrency(
            @Param("statuses") List<InvoiceStatus> statuses,
            @Param("user") User user,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}

