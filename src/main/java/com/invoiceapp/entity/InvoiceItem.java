package com.invoiceapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;

//invoice itme entity class

@Getter @Setter
@Entity
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Invoice invoice;

    private String description;
    private Integer quantity;
    @Column(nullable = false)
    @DecimalMin(value = "0.01", message = "Unit price must be at least 0.01")
    private BigDecimal unitPrice;

    public BigDecimal getAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
