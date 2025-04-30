package com.invoiceapp.controller.mvccontroller;

import com.invoiceapp.dto.invoice.InvoiceForm;
import com.invoiceapp.dto.invoice.InvoiceRequest;
import com.invoiceapp.dto.invoice.InvoiceResponse;
import com.invoiceapp.dto.invoice.RecordPaymentForm;
import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.ClientService;
import com.invoiceapp.service.InvoicePdfService;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.util.InvoiceMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class InvoiceAdminController {

    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final InvoicePdfService pdfService;
    private final UserProvider userProvider;

    //List all invoices
    @GetMapping
    public String list(@RequestParam Optional<InvoiceStatus> status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        invoiceService.markOverdue();  // auto-flip SENT→OVERDUE

        Page<InvoiceResponse> pg = invoiceService.list(status, page, 12);
        model.addAttribute("invoices", pg.getContent());
        model.addAttribute("page",     pg);              // for pagination bar
        model.addAttribute("filter",   status.orElse(null));
        return "admin/invoice-list";
    }

    //Send invoice
    @PostMapping("/{id}/send")
    public String send(@PathVariable Long id, RedirectAttributes ra) {
        invoiceService.send(id);
        ra.addFlashAttribute("success", "Invoice sent!");
        return "redirect:/admin/invoices";
    }

    //Get payment form
    @GetMapping("/{id}/record-payment")
    public String paymentForm(@PathVariable Long id, Model model) {
        InvoiceResponse invoice = invoiceService.get(id);
        RecordPaymentForm form = new RecordPaymentForm();
        form.setPaymentDate(LocalDate.now());
        model.addAttribute("invoice", invoice);
        model.addAttribute("form", form);
        return "admin/record-payment-form";
    }

    //Mark paid form
    @PostMapping("/{id}/mark-paid")
    public String submitPayment(@PathVariable Long id,
                                @Valid @ModelAttribute("form") RecordPaymentForm form,
                                BindingResult result,
                                Model model,
                                RedirectAttributes ra) {

        if (result.hasErrors()) {
            // re-populate the invoice so the form can render correctly
            model.addAttribute("invoice", invoiceService.get(id));
            return "admin/record-payment-form";
        }

        // invoiceService.markPaid should now also save form.getTransactionId()
        invoiceService.markPaid(id, form);

        ra.addFlashAttribute("success", "Payment recorded & invoice marked PAID.");
        return "redirect:/admin/invoices";
    }

    //Get new invoice form
    @GetMapping("/new")
    public String newForm(Model model,
                          @RequestParam Optional<Long> clientId) {

        InvoiceForm form = new InvoiceForm();
        clientId.ifPresent(form::setClientId);
        form.setDueDate(LocalDate.now().plusDays(14)); // NET-14 default

        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("form",    form);
        return "admin/invoice-form";
    }

    //Create new invoice
    @PostMapping
    public String submit(@Valid @ModelAttribute("form") InvoiceForm form,
                         BindingResult result,
                         RedirectAttributes ra) {

        if (result.hasErrors()) {
            return "admin/invoice-form";
        }

        invoiceService.create(InvoiceMapper.fromForm(form));
        ra.addFlashAttribute("success", "Draft invoice created!");
        return "redirect:/admin/invoices";
    }


    //Download pdf iinvoice
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generate(invoiceService.getEntity(id));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdf);
    }

    //Download receipt pdf
    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        Invoice inv = invoiceService.getEntity(id);
        byte[] pdf = pdfService.generateReceipt(inv);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("receipt-"+inv.getTransactionId()+".pdf")
                                .build().toString())
                .body(pdf);
    }
    //Edit invoice form
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        InvoiceResponse dto = invoiceService.get(id);
        InvoiceForm form = InvoiceMapper.toForm(dto);
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("form",    form);
        return "admin/invoice-form";
    }

    //Update invoice draft
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") InvoiceForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("clients", clientService.findAll());
            return "admin/invoice-form";
        }

        InvoiceRequest req = InvoiceMapper.fromForm(form);
        invoiceService.update(id, req);

        ra.addFlashAttribute("success", "Invoice updated!");
        return "redirect:/admin/invoices";
    }

    //Delete invoice
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        invoiceService.delete(id);
        ra.addFlashAttribute("success", "Invoice deleted!");
        return "redirect:/admin/invoices";
    }

    //Revert payment
    @PostMapping("/{id}/revert-payment")
    public String revertPayment(@PathVariable Long id,
                                RedirectAttributes ra) {
        invoiceService.revertPaymentStatus(id);
        ra.addFlashAttribute("success", "Invoice reverted back to SENT");
        return "redirect:/admin/invoices";
    }

}
