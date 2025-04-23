package com.invoiceapp.util;

import com.invoiceapp.dto.InvoiceItemResponse;
import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceItem;
import com.invoiceapp.entity.InvoiceStatus;
import org.springframework.stereotype.Component;

@Component
public final class InvoiceMapper {

    private InvoiceMapper() {}

    public static Invoice toEntity(InvoiceRequest dto, Client client) {
        Invoice inv = new Invoice();
        inv.setClient(client);
        inv.setDueDate(dto.dueDate());
        inv.setStatus(InvoiceStatus.DRAFT);

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
                        .toList()
        );
    }
}
