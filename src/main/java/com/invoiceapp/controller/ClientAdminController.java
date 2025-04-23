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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/clients")
@RequiredArgsConstructor
public class ClientAdminController {

    private final ClientService clientService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("clients", clientService.findAll());
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
