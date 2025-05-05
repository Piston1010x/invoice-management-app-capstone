package com.invoiceapp.controller;

import com.invoiceapp.dto.misc.DashboardStats;
import com.invoiceapp.dto.invoice.InvoiceResponse;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.DashboardService;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final DashboardService dashboardService;
    private final InvoiceService invoiceService;
    private final UserService userService;


    @GetMapping
    public String rootRedirect() {
        log.info("Redirecting to /admin/dashboard");
        return "redirect:/admin/dashboard";
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam("userId") Optional<Long> pick,
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Optional<LocalDate> fromDate,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Optional<LocalDate> toDate,
            HttpSession session,
            Model model
    ) {
        log.info("Accessing dashboard for user: {}", ud.getUsername());
        // date range
        LocalDate start = fromDate.orElse(LocalDate.now().withDayOfMonth(1));
        LocalDate end   = toDate.orElse(LocalDate.now());

        // who am I?
        User me = userService.findByEmail(ud.getUsername());
        log.info("Current user: {} with role: {}", me.getEmail(), me.getRole());

        // if admin & dropdown changed, store it
        if (me.getRole() == Role.ADMIN && pick.isPresent()) {
            log.info("Admin changing the view to user with ID: {}", pick.get());
            session.setAttribute("viewAsUserId", pick.get());
        }
        // pick target user
        Long sid = (Long) session.getAttribute("viewAsUserId");
        User target = (me.getRole() == Role.ADMIN && sid != null)
                ? userService.findById(sid)
                : me;

        log.info("Viewing dashboard for target user: {}", target.getEmail());


        // compute stats & recent invoices
        DashboardStats stats   = dashboardService.getStatsFor(target, start, end);
        List<InvoiceResponse> recent = invoiceService.getRecentInvoices(target.getEmail(), 5);

        // populate model
        model.addAttribute("stats", stats);
        model.addAttribute("recentInvoices", recent);
        model.addAttribute("from", start);
        model.addAttribute("to", end);

        if (me.getRole() == Role.ADMIN) {
            model.addAttribute("users", userService.findAllUsers());
            model.addAttribute("selectedUserId",target.getId());
        }
        log.info("Dashboard data populated for user: {}", target.getEmail());
        return "admin/dashboard";
    }
}
