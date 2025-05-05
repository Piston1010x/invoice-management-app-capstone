package com.invoiceapp.controller;

import com.invoiceapp.dto.misc.UserResponse;
import com.invoiceapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestMapping; // <-- Import this

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    // Admin only page to list all users
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(@RequestParam(defaultValue="0") int page,
                            @RequestParam(defaultValue="10") int size,
                            Model model) {
        log.info("Fetching user list with page: {}, size: {}", page, size);
        Page<UserResponse> pg = userService.listForAdmin(page, size);
        model.addAttribute("page", pg);
        model.addAttribute("users", pg.getContent());
        log.info("Fetched {} users for display.", pg.getContent().size());
        return "admin/user-list"; // Thymeleaf view name remains the same
    }

    //mapping for promoting a normal user account to an admin
    @PostMapping("/promote/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String promoteToAdmin(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        log.info("Admin promoting user with ID: {}", id);
        userService.promoteToAdmin(id);
        redirectAttributes.addFlashAttribute("success","User promoted to ADMIN.");
        log.info("User with ID: {} has been promoted to ADMIN.", id);
        return "redirect:/admin/users";
    }


    //demote an admin to normal user
    @PostMapping("/demote/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String demoteToUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        log.info("Admin demoting user with ID: {}", id);
        userService.demoteToUser(id);
        redirectAttributes.addFlashAttribute("success", "Admin rights removed.");
        log.info("User with ID: {} has been demoted to USER.", id);
        return "redirect:/admin/users";
    }
}