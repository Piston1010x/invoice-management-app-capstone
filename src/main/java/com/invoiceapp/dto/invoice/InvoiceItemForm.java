package com.invoiceapp.dto.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;


import java.math.BigDecimal;


//invoice item form: desc, qty, price

@Data
@NoArgsConstructor
public class InvoiceItemForm {
    private String description;
    @Min(value = 1)
    private Integer quantity;
    @DecimalMin(value = "0.01", inclusive = true, message = "Unit price must be positive")
    private BigDecimal unitPrice;

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
