package com.invoiceapp.service;

import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceItem;
import com.invoiceapp.entity.InvoiceStatus;
import com.itextpdf.text.BaseColor;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;

import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class InvoicePdfService {

    //generate invoice pdf
    public byte[] generate(Invoice inv) {
        log.info("Generating PDF for invoice {}", inv.getInvoiceNumber());
        Document doc = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            //add fulfilled watermark if invoice is paid
            if (inv.getStatus() == InvoiceStatus.PAID) {
                log.info("Adding watermark to invoice {}: 'FULFILLED'", inv.getInvoiceNumber());
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

            //headers and dates
            Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 12);

            if(inv.getStatus() != InvoiceStatus.DRAFT) {
                log.info("Adding invoice header for invoice {}", inv.getInvoiceNumber());
                doc.add(new Paragraph("Invoice " + inv.getInvoiceNumber(), h1));
            }
            doc.add(new Paragraph("Issue date: " + inv.getIssueDate(), normal));
            doc.add(new Paragraph("Due date: " + inv.getDueDate(), normal));
            doc.add(Chunk.NEWLINE);

            log.info("Adding items table for invoice {}", inv.getInvoiceNumber());
            //from name and to name tables
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

            //items table
            PdfPTable table = new PdfPTable(new float[]{3, 1, 2, 2});
            table.setWidthPercentage(100);
            addHeader(table, "Description", "Qty", "Unit Price", "Amount");

            //iterate through inv items and create corresponding tables

            for (InvoiceItem it : inv.getItems()) {
                log.info("Adding item to invoice {}: Description: {}, Qty: {}, Unit Price: {}, Amount: {}",
                        inv.getInvoiceNumber(), it.getDescription(), it.getQuantity(), it.getUnitPrice(), it.getAmount());
                table.addCell(it.getDescription());
                table.addCell(String.valueOf(it.getQuantity()));
                table.addCell(it.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toString());
                table.addCell(it.getAmount().setScale(2, RoundingMode.HALF_UP).toString());
            }

            PdfPCell totalCell = new PdfPCell(new Phrase("TOTAL", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            totalCell.setColspan(3);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalCell);

            String totalText = inv.getCurrency() + " " + inv.getTotal().setScale(2, RoundingMode.HALF_UP);
            table.addCell(totalText);

            doc.add(table);
            doc.close();

            log.info("PDF generated successfully for invoice {}", inv.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}: {}", inv.getInvoiceNumber(), e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }
        return baos.toByteArray();
    }


    //helper method to add headers
    private void addHeader(PdfPTable t, String... labels) {
        for (String l : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(l, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            t.addCell(cell);
        }
    }

    //method to generate receiot
    public byte[] generateReceipt(Invoice inv) {
        // use a receipt-friendly size & tighter margins
        log.info("Generating receipt PDF for invoice {}", inv.getInvoiceNumber());
        Document doc = new Document(PageSize.A6, 20, 20, 20, 20);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            //title
            Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Payment Receipt", h1);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(Chunk.NEWLINE);

            //info tx/inv number etc.
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{3, 2});
            info.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            info.getDefaultCell().setPadding(2);
            info.addCell(new Phrase("Invoice #: " + inv.getInvoiceNumber(), normal));
            info.addCell(new Phrase("Transaction ID: " + inv.getTransactionId(), normal));
            info.addCell(new Phrase("Date: " + inv.getPaymentDate(), normal));
            info.addCell(new Phrase("", normal)); // spacer
            doc.add(info);
            doc.add(Chunk.NEWLINE);

            //items
            PdfPTable table = new PdfPTable(new float[]{4,1,2});
            table.setWidthPercentage(100);
            table.setSpacingBefore(5f);

            Font hdr = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            BaseColor gray = new BaseColor(0xEE, 0xEE, 0xEE);
            Color color = new Color(0xFFFFFF);
            // header row
            for (String col : List.of("Description","Qty","Amount")) {
                PdfPCell cell = new PdfPCell(new Phrase(col, hdr));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(color);
                cell.setPadding(4f);
                table.addCell(cell);
            }

            // data rows
            for (var it : inv.getItems()) {
                log.info("Adding receipt item: Description: {}, Qty: {}, Amount: {}", it.getDescription(), it.getQuantity(), it.getAmount());
                PdfPCell desc = new PdfPCell(new Phrase(it.getDescription(), normal));
                desc.setPadding(4f);
                table.addCell(desc);

                PdfPCell qty = new PdfPCell(new Phrase(it.getQuantity().toString(), normal));
                qty.setHorizontalAlignment(Element.ALIGN_CENTER);
                qty.setPadding(4f);
                table.addCell(qty);

                PdfPCell amt = new PdfPCell(new Phrase(
                        it.getAmount().setScale(2, RoundingMode.HALF_UP).toString(), normal));
                amt.setHorizontalAlignment(Element.ALIGN_RIGHT);
                amt.setPadding(4f);
                table.addCell(amt);
            }

            // total row
            PdfPCell lbl = new PdfPCell(new Phrase("TOTAL", hdr));
            lbl.setColspan(2);
            lbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
            lbl.setPadding(4f);
            table.addCell(lbl);

            log.info("Adding total to receipt for invoice {}: {}", inv.getInvoiceNumber(), inv.getTotal().setScale(2, RoundingMode.HALF_UP));
            PdfPCell tot = new PdfPCell(new Phrase(inv.getTotal().setScale(2, RoundingMode.HALF_UP).toString(), hdr));
            tot.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tot.setPadding(4f);
            table.addCell(tot);

            doc.add(table);
            doc.close();
            log.info("Succesfully generated receipt PDF for invoice {}", inv.getInvoiceNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.info("failed to generate receipt");
            throw new RuntimeException("Couldn’t generate receipt PDF", e);
        }
    }
}
