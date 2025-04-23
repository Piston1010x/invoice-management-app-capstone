package com.invoiceapp.service;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceItem;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
@Service
public class InvoicePdfService {


    public byte[] generate(Invoice inv){
        Document doc = new Document(PageSize.A4);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 12);

            doc.add(new Paragraph("Invoice " + inv.getInvoiceNumber(), h1));
            doc.add(new Paragraph("Issue date: " + inv.getIssueDate(), normal));
            doc.add(new Paragraph("Due date: " + inv.getDueDate(), normal));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{3, 1, 2, 2});
            table.setWidthPercentage(100);
            addHeader(table, "Description", "Qty", "Unit Price", "Amount");


            for (InvoiceItem it : inv.getItems()) {
                table.addCell(it.getDescription());
                table.addCell(String.valueOf(it.getQuantity()));
                table.addCell(it.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toString());
                table.addCell(it.getAmount().setScale(2, RoundingMode.HALF_UP).toString());
            }

            PdfPCell total = new PdfPCell(new Phrase("TOTAL"));
            total.setColspan(3);
            total.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(total);
            table.addCell(inv.getTotal().setScale(2, RoundingMode.HALF_UP).toString());

            doc.add(table);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
        return baos.toByteArray();
    }

    private void addHeader(PdfPTable t, String... labels) {
        for (String l : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(l, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            t.addCell(cell);
        }
    }
}

