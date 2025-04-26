package com.invoiceapp.controller;

import com.invoiceapp.dto.DashboardStats;
import com.invoiceapp.dto.InvoiceResponse;
import com.invoiceapp.entity.User;
import com.invoiceapp.service.DashboardService;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final InvoiceService invoiceService;

    /** Redirect /admin → /admin/dashboard */
    @GetMapping
    public String rootRedirect() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Model model,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Optional<LocalDate> fromDate,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Optional<LocalDate> toDate
    ) {
        // Determine date range (default: first of month → today)
        LocalDate start = fromDate.orElse(LocalDate.now().withDayOfMonth(1));
        LocalDate end   = toDate.orElse(LocalDate.now());

        // Load user
        User user = userService.findByEmail(userDetails.getUsername());

        // Fetch stats for the given range
        DashboardStats stats = dashboardService.getStatsFor(user, start, end);

        // Fetch recent invoices (always last 5)
        List<InvoiceResponse> recentInvoices =
                invoiceService.getRecentInvoices(user.getEmail(), 5);

        // Populate model
        model.addAttribute("stats", stats);
        model.addAttribute("recentInvoices", recentInvoices);
        model.addAttribute("from", start);
        model.addAttribute("to", end);

        return "admin/dashboard";
    }
}
