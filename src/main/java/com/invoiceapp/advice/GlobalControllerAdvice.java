package com.invoiceapp.advice;

import com.invoiceapp.exception.ClientHasActiveInvoicesException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.AccessDeniedException;

// Global exception handler for client deletion errors
@Slf4j
@ControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(ClientHasActiveInvoicesException.class)
    // Redirects with an error message when deletion is blocked
    public String handleActiveInvoices(ClientHasActiveInvoicesException ex,
                                       RedirectAttributes ra) {
        log.error("Client has active invoices, deletion blocked. Active invoices: {}", ex.getCount());
        ra.addFlashAttribute("error",
                "Cannot delete client: " + ex.getCount() + " invoice(s) are SENT/OVERDUE.");
        return "redirect:/admin/clients";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes ra) {
        log.warn("Access denied exception: {}", ex.getMessage());
        //flash message
        ra.addFlashAttribute("error",
                "You don’t have permission to perform that action.");
        // redirect back to where they came from (or dashboard)
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/dashboard");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidationExceptions(ConstraintViolationException ex, RedirectAttributes ra) {
        // Collect all error messages from the validation exception
        StringBuilder errorMessage = new StringBuilder("Validation error(s):\n");
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errorMessage.append(violation.getMessage()).append("\n");
        }

        log.error("Validation error: {}", errorMessage.toString());

        // Flash the error message to the user
        ra.addFlashAttribute("error", errorMessage.toString());
        return "redirect:/admin/invoices";  // Redirect to the invoice list with the error message
    }
}
