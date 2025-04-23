package com.invoiceapp.controller;

import com.invoiceapp.dto.ClientForm;
import com.invoiceapp.dto.ClientRequest;
import com.invoiceapp.dto.ClientResponse;
import com.invoiceapp.service.ClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/clients")
@RequiredArgsConstructor
public class ClientAdminController {

    private final ClientService clientService;

    // src/main/java/com/invoiceapp/controller/ClientAdminController.java
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {

        Page<ClientResponse> pg = clientService.list(page, size);

        model.addAttribute("clients", pg.getContent());
        model.addAttribute("page",    pg);          // needed by the pagination bar
        return "admin/client-list";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        m.addAttribute("form", new ClientForm());
        return "admin/client-form";
    }



    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        clientService.delete(id); // you’ll implement this in the service/repo
        ra.addFlashAttribute("success", "Client deleted!");
        return "redirect:/admin/clients";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        ClientResponse dto = clientService.findById(id);

        // re-map into form (you could just reuse ClientResponse if fields match)
        ClientForm form = new ClientForm();
        form.setId(dto.id());
        form.setName(dto.name());
        form.setEmail(dto.email());
        form.setPhone(dto.phone());

        model.addAttribute("form", form);
        return "admin/client-form";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") ClientForm form,
                         RedirectAttributes ra) {
        if (form.getId() == null) {
            clientService.create(new ClientRequest(form.getName(), form.getEmail(), form.getPhone()));
            ra.addFlashAttribute("success", "Client created!");
        } else {
            clientService.update(form);
            ra.addFlashAttribute("success", "Client updated!");
        }

        return "redirect:/admin/clients";
    }

}
