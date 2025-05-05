package com.invoiceapp.controller;

import com.invoiceapp.dto.invoice.*;
import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.ClientService;
import com.invoiceapp.service.InvoicePdfService;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.service.UserService;
import com.invoiceapp.util.InvoiceMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class InvoiceAdminController {

    private final InvoiceService invoiceService;
    private final ClientService  clientService;
    private final UserProvider userProvider;
    private final UserService userService;
    private final InvoicePdfService pdfService;
    private final InvoiceRepository invoiceRepository;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    public String list(
            @RequestParam Optional<InvoiceStatus> status,
            @RequestParam(defaultValue="0") int page,
            HttpSession session,
            Model model
    ) {
        log.info("Fetching invoice list with status: {} for page: {} and size: 12", status.orElse(null), page);

        // flip overdue
        invoiceService.markOverdue();


        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole()==Role.ADMIN && sessionId !=null)
                ? userService.findById(sessionId) : me;

        // fetch
        Page<InvoiceResponse> pg = invoiceService.listForUser(target, status, page, 12);
        log.info("Fetched {} invoices for user: {}", pg.getContent().size(), target.getEmail());

        model.addAttribute("invoices",       pg.getContent());
        model.addAttribute("page",           pg);
        model.addAttribute("filter",         status.orElse(null));
        if (me.getRole()==Role.ADMIN) {
            model.addAttribute("users",          userService.findAllUsers());
            model.addAttribute("selectedUserId", target.getId());
        }
        return "admin/invoice-list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/send")
    public String send(@PathVariable Long id,
                       RedirectAttributes redirectAttributes) {
        log.info("Sending invoice with ID: {}", id);
        invoiceService.send(id);
        redirectAttributes.addFlashAttribute("success","Invoice sent!");
        return "redirect:/admin/invoices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/record-payment")
    public String paymentForm(@PathVariable Long id, Model model) {
        log.info("Displaying payment form for invoice with ID: {}", id);
        model.addAttribute("invoice", invoiceService.get(id));
        RecordPaymentForm form = new RecordPaymentForm();
        form.setPaymentDate(LocalDate.now());
        model.addAttribute("form", form);
        return "admin/record-payment-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/mark-paid")
    public String markPaid(@PathVariable Long id,
                           @Valid @ModelAttribute("form") RecordPaymentForm form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("Errors found in payment form for invoice with ID: {}", id);
            model.addAttribute("invoice", invoiceService.get(id));
            return "admin/record-payment-form";
        }
        log.info("Recording payment for invoice with ID: {} and marking as PAID", id);
        invoiceService.markPaid(id, form);
        redirectAttributes.addFlashAttribute("success","Payment recorded & marked PAID.");
        return "redirect:/admin/invoices";
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sessionId != null)
                ? userService.findById(sessionId) : me;
        log.info("Displaying new invoice form for user: {}", target.getEmail());

        InvoiceForm form = new InvoiceForm();
        form.setDueDate(LocalDate.now().plusDays(14));

        model.addAttribute("form",           form);
        model.addAttribute("clients",        clientService.findAllForUser(target));
        if (me.getRole()==Role.ADMIN) {
            model.addAttribute("users",       userService.findAllUsers());
            model.addAttribute("selectedUserId", target.getId());
        }
        return "admin/invoice-form";
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("form") InvoiceForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sessionId != null)
                ? userService.findById(sessionId) : me;

        if (bindingResult.hasErrors()) {
            log.warn("Form submission has errors for new invoice by user: {}", target.getEmail());
            //Repopulate model attributes for the form view
            model.addAttribute("clients", clientService.findAllForUser(target));
            if (me.getRole() == Role.ADMIN) {
                model.addAttribute("users", userService.findAllUsers());
                model.addAttribute("selectedUserId", target.getId());
            }
            //Re-populate model attributes
            return "admin/invoice-form";
        }

        log.info("Creating a new invoice for user: {} with details: {}", target.getEmail(), form);
        InvoiceRequest invoiceRequest = InvoiceMapper.fromForm(form);
        invoiceService.createForUser(invoiceRequest, target);

        redirectAttributes.addFlashAttribute("success", "Draft invoice created!");
        return "redirect:/admin/invoices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole()==Role.ADMIN && sessionId !=null)
                ? userService.findById(sessionId) : me;
        log.info("Displaying edit form for invoice with ID: {} by user: {}", id, target.getEmail());
        InvoiceResponse dto = invoiceService.get(id);
        InvoiceForm form = InvoiceMapper.toForm(dto);

        model.addAttribute("form",      form);
        model.addAttribute("clients",   clientService.findAllForUser(target));
        model.addAttribute("users",     userService.findAllUsers());
        model.addAttribute("selectedUserId", target.getId());
        return "admin/invoice-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") InvoiceForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            Model model
    ) {
        User me = userProvider.getCurrentUser();
        Long sid = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sid != null)
                ? userService.findById(sid) : me;

        if (bindingResult.hasErrors()) {
            log.warn("Form submission has errors while updating invoice with ID: {} for user: {}", id, target.getEmail());
            model.addAttribute("clients", clientService.findAllForUser(target)); // Use appropriate user for client list
            if (me.getRole() == Role.ADMIN) {
                model.addAttribute("users", userService.findAllUsers());
                model.addAttribute("selectedUserId", target.getId()); // Or invoiceOwner.getId()?
            }
            model.addAttribute("invoiceId", id);
            return "admin/invoice-form";
        }

        log.info("Updating invoice with ID: {} for user: {}", id, target.getEmail());
        InvoiceRequest request = InvoiceMapper.fromForm(form);

        invoiceService.updateForUser(id, request, target);

        redirectAttributes.addFlashAttribute("success", "Invoice updated!");
        return "redirect:/admin/invoices";
    }


    //delete method
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteInvoice(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
            if (invoice.getStatus() != InvoiceStatus.DRAFT) {
                throw new IllegalStateException("Only draft invoices can be deleted.");
            }
            invoiceRepository.delete(invoice);
            redirectAttributes.addFlashAttribute("successToast", "Invoice deleted.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorToast", e.getMessage());
        }
        return "redirect:/admin/invoices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/revert-payment")
    public String revert(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Reverting payment status for invoice with ID: {}", id);
        invoiceService.revertPaymentStatus(id);
        redirectAttributes.addFlashAttribute("success","Reverted to SENT");
        return "redirect:/admin/invoices";
    }


    //Download pdf invoice
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        log.info("Downloading PDF for invoice with ID: {}", id);
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
        log.info("Downloading receipt PDF for invoice with ID: {} and transaction ID: {}", id, inv.getTransactionId());
        byte[] pdf = pdfService.generateReceipt(inv);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("receipt-"+inv.getTransactionId()+".pdf")
                                .build().toString())
                .body(pdf);
    }

}
