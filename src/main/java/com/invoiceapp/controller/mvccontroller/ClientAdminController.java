package com.invoiceapp.controller.mvccontroller;

import com.invoiceapp.dto.client.ClientForm;
import com.invoiceapp.dto.client.ClientRequest;
import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/clients")
@RequiredArgsConstructor
public class ClientAdminController {

    private final ClientService clientService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        Page<ClientResponse> pg = clientService.list(page, size);
        model.addAttribute("clients", pg.getContent());
        model.addAttribute("page", pg);
        return "admin/client-list";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        ClientForm form = new ClientForm();
        form.setCountryCode("+995");  // default country
        m.addAttribute("form", form);
        return "admin/client-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        ClientResponse dto = clientService.findById(id);
        ClientForm form = new ClientForm();
        form.setId(dto.id());
        form.setName(dto.name());
        form.setEmail(dto.email());
        // split existing phone into code + raw
        String phone = dto.phone();
        if (phone != null && phone.startsWith("+")) {
            String code = phone.replaceFirst("^\\+(\\d{1,3}).*$", "+$1");
            form.setCountryCode(code);
            form.setRawPhone(phone.substring(code.length()));
        } else {
            form.setCountryCode("+995");
            form.setRawPhone(phone != null ? phone : "");
        }
        m.addAttribute("form", form);
        return "admin/client-form";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") ClientForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {

        if (result.hasErrors()) {
            return "admin/client-form";
        }

        // combine code + raw into final phone
        String fullPhone = form.getCountryCode() + form.getRawPhone();
        form.setPhone(fullPhone);

        try {
            ClientRequest req = new ClientRequest(
                    form.getName(),
                    form.getEmail(),
                    fullPhone
            );

            if (form.getId() == null) {
                clientService.create(req);
                ra.addFlashAttribute("success", "Client created!");
            } else {
                clientService.update(form);
                ra.addFlashAttribute("success", "Client updated!");
            }
            return "redirect:/admin/clients";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("formError", ex.getMessage());
            return "admin/client-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        clientService.delete(id);
        ra.addFlashAttribute("success", "Client deleted!");
        return "redirect:/admin/clients";
    }
}
