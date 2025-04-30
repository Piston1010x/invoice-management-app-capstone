package com.invoiceapp.dto.invoice;

import com.invoiceapp.entity.Currency;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//invoice form dto

@Setter
@Getter
public class InvoiceForm {
    private Long clientId;
    @NotNull @FutureOrPresent
    private LocalDate dueDate;
    @NotNull
    private Currency currency;
    @NotBlank
    private String toName;
    @NotBlank
    private String fromName;
    @NotBlank
    private String bankName;
    @NotBlank
    private String iban;

    // ← updated:
    private List<InvoiceItemForm> items = new ArrayList<>();

    // getters & setters for all fields, including items
    public List<InvoiceItemForm> getItems() { return items; }
    public void setItems(List<InvoiceItemForm> items) { this.items = items; }
}
