package com.invoiceapp.controller;

import com.invoiceapp.dto.DashboardStats;
import com.invoiceapp.entity.User;
import com.invoiceapp.service.DashboardService;
import com.invoiceapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// com.invoiceapp.controller.AdminHomeController
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final DashboardService dashboardService;
    private final UserService userService;
    /* GET /admin  ->  redirect to /admin/dashboard for cleanliness */
    @GetMapping
    public String rootRedirect() {
        return "redirect:admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        DashboardStats stats = dashboardService.getStatsFor(user);
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }
}
