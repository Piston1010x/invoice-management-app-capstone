package com.invoiceapp;

import com.invoiceapp.entity.Currency;
import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceItem;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.service.InvoicePdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class InvoicePdfServiceTest {

    @Autowired
    private InvoicePdfService invoicePdfService;

    private Invoice invoice;

    @BeforeEach
    public void setup() {
        // Create an Invoice with mock data
        invoice = new Invoice();
        invoice.setInvoiceNumber("INV123");
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setCurrency(Currency.USD);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setToName("Client Name");
        invoice.setFromName("Company Name");
        invoice.setBankName("Bank Name");
        invoice.setIban("1234567890");
        invoice.setTotal(BigDecimal.valueOf(200));

        // Create and set items for the invoice
        InvoiceItem item1 = new InvoiceItem();
        item1.setDescription("Item 1");
        item1.setQuantity(2);
        item1.setUnitPrice(BigDecimal.valueOf(50));

        InvoiceItem item2 = new InvoiceItem();
        item2.setDescription("Item 2");
        item2.setQuantity(3);
        item2.setUnitPrice(BigDecimal.valueOf(30));

        invoice.setItems(Arrays.asList(item1, item2));
    }

    @Test
    public void testGeneratePdf() {
        // Generate the PDF
        byte[] pdfBytes = invoicePdfService.generate(invoice);

        // Check that the generated PDF is not null or empty
        assertNotNull(pdfBytes, "Generated PDF should not be null");
        assertTrue(pdfBytes.length > 0, "Generated PDF should not be empty");

        // Optionally, further PDF content validation could be done
        // e.g., validating specific content in the PDF, like the invoice number
        // but it's difficult to test without reading the actual content of the PDF.
    }

    @Test
    public void testGenerateReceipt() {
        // Generate the receipt PDF
        byte[] receiptBytes = invoicePdfService.generateReceipt(invoice);

        // Check that the generated receipt is not null or empty
        assertNotNull(receiptBytes, "Generated receipt should not be null");
        assertTrue(receiptBytes.length > 0, "Generated receipt should not be empty");
    }
}
