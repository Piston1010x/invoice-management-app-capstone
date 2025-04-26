package com.invoiceapp.service;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceItem;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;

@Service
public class InvoicePdfService {

    public byte[] generate(Invoice inv) {
        Document doc = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            // ─── Watermark if PAID ──────────────────────────────
            if (inv.getStatus() == InvoiceStatus.PAID) {
                PdfContentByte canvas = writer.getDirectContentUnder();
                Font wm = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 60, new GrayColor(0.85f));
                Phrase watermark = new Phrase("FULFILLED", wm);
                ColumnText.showTextAligned(
                        canvas,
                        Element.ALIGN_CENTER,
                        watermark,
                        298, 421,    // center of A4
                        45           // rotation angle
                );
            }

            // ─── Header & dates ─────────────────────────────────
            Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            doc.add(new Paragraph("Invoice " + inv.getInvoiceNumber(), h1));
            doc.add(new Paragraph("Issue date: " + inv.getIssueDate(), normal));
            doc.add(new Paragraph("Due date: " + inv.getDueDate(), normal));
            doc.add(Chunk.NEWLINE);

            // ─── From / To table ────────────────────────────────
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            PdfPCell fromCell = new PdfPCell();
            fromCell.setBorder(Rectangle.NO_BORDER);
            fromCell.addElement(new Paragraph("From:", normal));
            fromCell.addElement(new Paragraph(inv.getFromName(), normal));
            fromCell.addElement(new Paragraph(inv.getBankName(), normal));
            fromCell.addElement(new Paragraph("IBAN: " + inv.getIban(), normal));

            PdfPCell toCell = new PdfPCell();
            toCell.setBorder(Rectangle.NO_BORDER);
            toCell.addElement(new Paragraph("To:", normal));
            toCell.addElement(new Paragraph(inv.getToName(), normal));

            header.addCell(fromCell);
            header.addCell(toCell);
            doc.add(header);
            doc.add(Chunk.NEWLINE);

            // ─── Items table ────────────────────────────────────
            PdfPTable table = new PdfPTable(new float[]{3,1,2,2});
            table.setWidthPercentage(100);
            addHeader(table, "Description","Qty","Unit Price","Amount");

            for (InvoiceItem it : inv.getItems()) {
                table.addCell(it.getDescription());
                table.addCell(String.valueOf(it.getQuantity()));
                table.addCell(it.getUnitPrice().setScale(2,RoundingMode.HALF_UP).toString());
                table.addCell(it.getAmount().setScale(2,RoundingMode.HALF_UP).toString());
            }

            PdfPCell totalCell = new PdfPCell(new Phrase("TOTAL", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            totalCell.setColspan(3);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalCell);

            String totalText = inv.getCurrency() + " " + inv.getTotal().setScale(2,RoundingMode.HALF_UP);
            table.addCell(totalText);

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
