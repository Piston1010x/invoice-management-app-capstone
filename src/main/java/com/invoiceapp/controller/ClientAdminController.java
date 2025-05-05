package com.invoiceapp.controller;

import com.invoiceapp.dto.client.ClientForm;
import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.ClientService;
import com.invoiceapp.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/clients")
@RequiredArgsConstructor
public class ClientAdminController {

    private final ClientService clientService;
    private final UserProvider userProvider;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session,
            Model model) {

        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sessionId != null) 
                ? userService.findById(sessionId) : me;

        log.info("Fetching clients list for user: {} with page: {}, size: {}", target.getEmail(), page, size);
        Page<ClientResponse> pageForUser = clientService.listForUser(target, page, size);

        model.addAttribute("clients",        pageForUser.getContent());
        model.addAttribute("page", pageForUser);

        if (me.getRole() == Role.ADMIN) {
            model.addAttribute("users",          userService.findAllUsers());
            model.addAttribute("selectedUserId", target.getId());
        }
        log.info("Fetched {} clients for user: {}", pageForUser.getContent().size(), target.getEmail());
        return "admin/client-list";
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {

        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sessionId != null)
                ? userService.findById(sessionId) : me;

        log.info("Displaying new client form for user: {}", target.getEmail());
        ClientForm form = new ClientForm();
        form.setCountryCode("+995");

        model.addAttribute("form",form);
        if (me.getRole() == Role.ADMIN) {
            model.addAttribute("users",          userService.findAllUsers());
            model.addAttribute("selectedUserId", target.getId());
        }
        return "admin/client-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, HttpSession session, Model model) {

        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sessionId != null)
                ? userService.findById(sessionId) : me;

        log.info("Editing client with ID: {} for user: {}", id, target.getEmail());
        ClientResponse dto = clientService.findByIdForUser(id, target);
        ClientForm     form = new ClientForm();
        form.setId(dto.id());
        form.setName(dto.name());
        form.setEmail(dto.email());
        form.setPhone(dto.phone());

        model.addAttribute("form", form);
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("selectedUserId", target.getId());
        return "admin/client-form";
    }


    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("form") ClientForm form,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Combine country code and raw phone number
        form.setPhone(form.getCountryCode() + form.getRawPhone());

        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sessionId != null)
                ? userService.findById(sessionId) : me;

        log.info("Creating new client for user: {} with name: {}", target.getEmail(), form.getName());

        try {
            // The validation and creation are handled by the service layer
            clientService.createForUser(form, target);  // This triggers validateUnique
            redirectAttributes.addFlashAttribute("success", "Client created!");
            return "redirect:/admin/clients";  // Redirect after successful creation
        } catch (IllegalArgumentException ex) {
            // Catch any validation error (e.g., duplicate email/phone) and show the error message
            model.addAttribute("formError", ex.getMessage());
            model.addAttribute("users", userService.findAllUsers());
            model.addAttribute("selectedUserId", target.getId());
            return "admin/client-form";  // Return to the form with the error message
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User me = userProvider.getCurrentUser();
        Long sessionId = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sessionId != null)
                ? userService.findById(sessionId)
                : me;


        log.info("Deleting client with ID: {} for user: {}", id, target.getEmail());
        clientService.deleteForUser(id, target);
        redirectAttributes.addFlashAttribute("success", "Client deleted!");
        return "redirect:/admin/clients";
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ClientForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User me     = userProvider.getCurrentUser();
        Long sessionId    = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sessionId != null)
                ? userService.findById(sessionId) : me;

        // combine country code + raw phone into the 'phone' field
        form.setPhone(form.getCountryCode() + form.getRawPhone());

        if (bindingResult.hasErrors()) {
            log.warn("Form submission has errors for client ID: {}", id);
            model.addAttribute("formError", "Please fix the errors below.");
            model.addAttribute("users",          userService.findAllUsers());
            model.addAttribute("selectedUserId", target.getId());
            return "admin/client-form";
        }

        try {
            log.info("Updating client with ID: {} for user: {}", id, target.getEmail());
            clientService.updateForUser(form, target);
            redirectAttributes.addFlashAttribute("success", "Client updated!");
            return "redirect:/admin/clients";
        } catch (IllegalArgumentException ex) {
            // validation (duplicates) failed
            log.error("Error updating client with ID: {} for user {}: {}", id, target.getEmail(), ex.getMessage());
            model.addAttribute("formError", ex.getMessage());
            model.addAttribute("users",          userService.findAllUsers());
            model.addAttribute("selectedUserId", target.getId());
            return "admin/client-form";
        }
    }

}
