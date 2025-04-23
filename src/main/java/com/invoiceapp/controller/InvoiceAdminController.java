package com.invoiceapp.controller;

import com.invoiceapp.dto.InvoiceForm;
import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.service.ClientService;
import com.invoiceapp.service.InvoicePdfService;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.util.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class InvoiceAdminController {

    private final InvoiceService    invoiceService;
    private final ClientService     clientService;
    private final InvoicePdfService pdfService;      // for download

    /* ---------- LIST (optionally filtered) ---------- */
    @GetMapping
    public String list(@RequestParam Optional<InvoiceStatus> status,
                       Model model) {

        model.addAttribute("invoices", invoiceService.list(status));
        model.addAttribute("filter",   status.orElse(null));   // keep UI state
        return "admin/invoice-list";
    }

    /* ---------- SEND / MARK-PAID ---------- */
    @PostMapping("/{id}/send")
    public String send(@PathVariable Long id, RedirectAttributes ra) {
        invoiceService.send(id);
        ra.addFlashAttribute("success", "Invoice sent!");
        return "redirect:/admin/invoices";
    }

    @PostMapping("/{id}/mark-paid")
    public String markPaid(@PathVariable Long id, RedirectAttributes ra) {
        invoiceService.markPaid(id);
        ra.addFlashAttribute("success", "Invoice marked paid!");
        return "redirect:/admin/invoices";
    }

    /* ---------- NEW FORM ---------- */
    @GetMapping("/new")
    public String newForm(Model model,
                          @RequestParam Optional<Long> clientId) {

        InvoiceForm form = new InvoiceForm();
        clientId.ifPresent(form::setClientId);
        form.setDueDate(LocalDate.now().plusDays(14));      // default NET-14

        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("form",    form);
        return "admin/invoice-form";
    }

    /* ---------- CREATE (unlimited items) ---------- */
    @PostMapping
    public String submit(@ModelAttribute("form") InvoiceForm form,
                         RedirectAttributes ra) {

        InvoiceRequest req = InvoiceMapper.fromForm(form);
        invoiceService.create(req);

        ra.addFlashAttribute("success", "Draft invoice created!");
        return "redirect:/admin/invoices";
    }

    /* ---------- PDF DOWNLOAD ---------- */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {

        byte[] pdf = pdfService.generate(invoiceService.getEntity(id)); // helper that returns Invoice

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("invoice-" + id + ".pdf")
                                .build().toString())
                .body(pdf);
    }
}
