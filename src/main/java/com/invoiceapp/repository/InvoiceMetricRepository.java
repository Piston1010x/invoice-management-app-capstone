package com.invoiceapp.repository;
import com.invoiceapp.entity.InvoiceMetric;
import org.springframework.data.jpa.repository.JpaRepository;


//repo class for invoiceMetric entity
public interface InvoiceMetricRepository extends JpaRepository<InvoiceMetric,Long> {}

