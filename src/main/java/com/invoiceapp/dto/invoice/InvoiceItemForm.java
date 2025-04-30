package com.invoiceapp.dto.invoice;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

//invoice item form: desc, qty, price

public class InvoiceItemForm {
    private String description;
    private Integer quantity;
    @DecimalMin(value="0.01", inclusive=false)
    private BigDecimal unitPrice;

    public InvoiceItemForm() {}

    public InvoiceItemForm(String description, Integer quantity, BigDecimal unitPrice) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // getters & setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
