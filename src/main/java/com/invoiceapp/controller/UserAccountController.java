package com.invoiceapp.controller;

import com.invoiceapp.dto.misc.UserPasswordResetForm;
import com.invoiceapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserService userService;


    //get reset pass page
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/reset-password")
    public String showResetForm(Model model) {
        log.info("Displaying password reset form for user");
        model.addAttribute("form", new UserPasswordResetForm());
        return "user/reset-password";
    }



    //post for password change
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/reset-password")
    public String doReset(
            @Valid @ModelAttribute("form") UserPasswordResetForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Attempting to reset password for user: {}", principal.getUsername());
        // basic validation
        if (bindingResult.hasErrors() || !form.getNewPassword().equals(form.getConfirmPassword())) {
            if (!form.getNewPassword().equals(form.getConfirmPassword())) {
                bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords must match");
                log.warn("Password confirmation mismatch for user: {}", principal.getUsername());
            }
            return "user/reset-password";
        }

        try {
            userService.changePassword(
                    principal.getUsername(),
                    form.getOldPassword(),
                    form.getNewPassword()
            );
            log.info("Password successfully updated for user: {}", principal.getUsername());

        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("oldPassword", "incorrect", ex.getMessage());
            log.error("Password change failed for user {}: {}", principal.getUsername(), ex.getMessage());
            return "user/reset-password";
        }

        redirectAttributes.addFlashAttribute("success", "Password updated.");
        log.info("Password reset success for user: {}", principal.getUsername());
        return "redirect:/user/reset-password?success";
    }

}
