package com.invoiceapp.controller;

import com.invoiceapp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// com.invoiceapp.controller.AdminHomeController
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final DashboardService dash;

    /* GET /admin  ->  redirect to /admin/dashboard for cleanliness */
    @GetMapping
    public String rootRedirect() {
        return "redirect:/admin/dashboard";
    }

    /* GET /admin/dashboard */
    @GetMapping("/dashboard")
    public String dashboard(Model m) {
        m.addAttribute("stats", dash.snapshot());
        return "admin/dashboard";   // resolves to templates/admin/dashboard.html
    }
}
