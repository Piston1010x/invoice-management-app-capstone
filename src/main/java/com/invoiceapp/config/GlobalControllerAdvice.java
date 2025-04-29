// src/main/java/com/invoiceapp/config/GlobalControllerAdvice.java
package com.invoiceapp.config;

import com.invoiceapp.exception.ClientHasActiveInvoicesException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Global exception handler for client deletion errors
@ControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(ClientHasActiveInvoicesException.class)
    // Redirects with an error message when deletion is blocked
    public String handleActiveInvoices(ClientHasActiveInvoicesException ex,
                                       RedirectAttributes ra) {
        ra.addFlashAttribute("error",
                "Cannot delete client: " + ex.getCount() + " invoice(s) are SENT/OVERDUE.");
        return "redirect:/admin/clients";
    }
}
