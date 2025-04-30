package com.invoiceapp.util;

import com.invoiceapp.dto.invoice.*;
import com.invoiceapp.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public final class InvoiceMapper {
    private InvoiceMapper() {}


    //Maps invoice request (dto) to invoice (entity)
    public static Invoice toEntity(InvoiceRequest dto, Client client) {
        Invoice inv = new Invoice();
        inv.setClient(client);
        inv.setDueDate(dto.dueDate());
        inv.setCurrency(dto.currency());
        inv.setToName(dto.toName());
        inv.setFromName(dto.fromName());
        inv.setBankName(dto.bankName());
        inv.setIban(dto.iban());
        dto.items().forEach(i -> {
            InvoiceItem entityItem = new InvoiceItem();
            entityItem.setInvoice(inv);
            entityItem.setDescription(i.description());
            entityItem.setQuantity(i.quantity());
            entityItem.setUnitPrice(i.unitPrice());
            inv.getItems().add(entityItem);
        });
        return inv;
    }

    //Maps invoice (entity) to invoice (dto)
    public static InvoiceResponse toDto(Invoice inv) {
        return new InvoiceResponse(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getClient().getId(),
                inv.getClient().getName(),
                inv.getStatus(),
                inv.getIssueDate(),
                inv.getDueDate(),
                inv.getTotal(),
                inv.getItems().stream()
                        .map(it -> new InvoiceItemResponse(
                                it.getDescription(),
                                it.getQuantity(),
                                it.getUnitPrice(),
                                it.getAmount()))
                        .toList(),
                inv.getCurrency(),
                inv.getToName(),
                inv.getFromName(),
                inv.getBankName(),
                inv.getIban()
        );
    }


    //Converts UI form to InvoiceRequest object
    public static InvoiceRequest fromForm(InvoiceForm f) {
        List<InvoiceItemRequest> items = f.getItems().stream()
                .filter(r -> r.getDescription() != null && !r.getDescription().isBlank())
                .map(r -> new InvoiceItemRequest(
                        r.getDescription(),
                        r.getQuantity() == null ? 1 : r.getQuantity(),
                        r.getUnitPrice() == null ? BigDecimal.ZERO : r.getUnitPrice()))
                .toList();

        return new InvoiceRequest(
                f.getClientId(),
                items,
                f.getDueDate(),
                f.getCurrency(),
                f.getToName(),
                f.getFromName(),
                f.getBankName(),
                f.getIban()
        );

}

    //Converts InvoiceResponse form to UI form
    public static InvoiceForm toForm(InvoiceResponse dto) {
        InvoiceForm f = new InvoiceForm();
        f.setId(dto.id());               // ← copy the PK into the form
        f.setClientId(dto.clientId());
        f.setDueDate(dto.dueDate());
        f.setCurrency(dto.currency());
        f.setToName(dto.toName());
        f.setFromName(dto.fromName());
        f.setBankName(dto.bankName());
        f.setIban(dto.iban());
        f.setItems(dto.items().stream()
                .map(it -> new InvoiceItemForm(
                        it.description(),
                        it.quantity(),
                        it.unitPrice()))
                .toList());
        return f;
    }


}
