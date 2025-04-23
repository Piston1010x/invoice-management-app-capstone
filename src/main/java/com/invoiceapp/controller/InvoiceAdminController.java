
package com.invoiceapp.controller;

import com.invoiceapp.dto.InvoiceForm;
import com.invoiceapp.dto.InvoiceItemRequest;
import com.invoiceapp.dto.InvoiceRequest;
import com.invoiceapp.service.ClientService;
import com.invoiceapp.service.InvoiceService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class InvoiceAdminController {

    private final InvoiceService service;
    private final ClientService clientService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", service.list(java.util.Optional.empty()));
        return "admin/invoice-list";
    }



    @PostMapping("/{id}/send")
    public String send(@PathVariable Long id, RedirectAttributes ra) {
        service.send(id);
        ra.addFlashAttribute("success", "Invoice sent!");
        return "redirect:/admin/invoices";
    }

    @PostMapping("/{id}/mark-paid")
    public String markPaid(@PathVariable Long id, RedirectAttributes ra) {
        service.markPaid(id);
        ra.addFlashAttribute("success", "Invoice marked paid!");
        return "redirect:/admin/invoices";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("form", new InvoiceForm());
        return "admin/invoice-form";
    }

    @PostMapping
    public String create(@ModelAttribute InvoiceForm form,
                         RedirectAttributes ra) {

        List<InvoiceItemRequest> items = new ArrayList<>();

        Stream.of(
                        Tuple.of(form.getDesc1(), form.getQty1(), form.getPrice1()),
                        Tuple.of(form.getDesc2(), form.getQty2(), form.getPrice2()),
                        Tuple.of(form.getDesc3(), form.getQty3(), form.getPrice3())
                )
                .filter(t -> t._1() != null && !t._1().isBlank())
                .forEach(t -> items.add(new InvoiceItemRequest(
                        t._1(),
                        t._2() == null ? 1 : t._2(),
                        t._3() == null ? BigDecimal.ZERO : t._3()
                )));

        InvoiceRequest req = new InvoiceRequest(
                form.getClientId(),
                items,
                LocalDate.now().plusDays(14)
        );

        service.create(req);
        ra.addFlashAttribute("success", "Draft invoice created!");
        return "redirect:/admin/invoices";
    }

    // tiny tuple helper
    private record Tuple(String _1, Integer _2, BigDecimal _3) {
        static Tuple of(String d, Integer q, BigDecimal p) {
            return new Tuple(d, q, p);
        }
    }

}
