package com.invoiceapp.dto;

import com.invoiceapp.entity.Currency;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Setter
@Getter
public class InvoiceForm {
    private Long clientId;
    @NotNull @FutureOrPresent
    private LocalDate dueDate;
    private Currency currency;
    private String toName;
    private String fromName;
    private String bankName;
    private String iban;

    // ← updated:
    private List<InvoiceItemForm> items = new ArrayList<>();

    // getters & setters for all fields, including items
    public List<InvoiceItemForm> getItems() { return items; }
    public void setItems(List<InvoiceItemForm> items) { this.items = items; }
}
