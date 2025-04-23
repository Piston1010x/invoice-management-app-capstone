// src/main/java/com/invoiceapp/dto/InvoiceForm.java
package com.invoiceapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class InvoiceForm {

    private Long clientId;

    // NEW: due date picker binds straight to LocalDate
    private LocalDate dueDate;

    // NEW: front-end sends many rows => simple container
    private List<ItemRow> items = new ArrayList<>();

    @Data
    public static class ItemRow {
        private String description;
        private Integer quantity = 1;
        private BigDecimal unitPrice = BigDecimal.ZERO;
    }
}
