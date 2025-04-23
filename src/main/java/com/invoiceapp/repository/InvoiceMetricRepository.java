package com.invoiceapp.repository;

import com.invoiceapp.entity.InvoiceMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceMetricRepository extends JpaRepository<InvoiceMetric,Long> {

}

