// src/main/java/com/invoiceapp/controller/AuthController.java
package com.invoiceapp.controller;

import com.invoiceapp.dto.misc.RegisterForm;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    //Registration form
    @GetMapping("/register")
    public String registerForm(Model model) {
        log.info("Displaying registration form");
        model.addAttribute("form", new RegisterForm(null,null,null));
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("form") @Valid RegisterForm form,
                             BindingResult errors,
                             Model model) {
        log.info("Processing registration for email: {}", form.email());

        // Check if passwords match
        if (!form.password().equals(form.passwordConfirm())) {
            log.warn("Passwords do not match for email: {}", form.email());
            errors.rejectValue("passwordConfirm", null, "Passwords don’t match");
        }

        // Check if email already exists
        if (repo.existsByEmail(form.email())) {
            log.warn("Email already registered: {}", form.email());
            errors.rejectValue("email", null, "Email already registered");
        }

        // If there are any validation errors, return the form with errors
        if (errors.hasErrors()) {
            log.error("Registration failed for email: {}", form.email());
            return "register";  // Returns to the registration page with errors
        }

        // Save the new user in the database
        repo.save(new User(
                null,
                form.email(),
                encoder.encode(form.password()),  // Encoding the password before saving
                Role.USER,
                true  // Assuming user is active by default
        ));

        log.info("Successfully registered user with email: {}", form.email());

        //success message and redirect to login page
        model.addAttribute("success", "Account created – you can now log in.");
        return "redirect:/login";  // Use redirect to avoid resubmission issues on page refresh
    }


    //Login page
    @GetMapping("/login")
    public String loginPage() {
        log.info("Displaying login page");
        return "login";
    }
}
