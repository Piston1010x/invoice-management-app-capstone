package com.invoiceapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminHomeController {
    @GetMapping("/dashboard")
    public String home() {
        return "admin/dashboard"; }
}
