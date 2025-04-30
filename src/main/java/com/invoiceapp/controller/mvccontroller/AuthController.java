// src/main/java/com/invoiceapp/controller/AuthController.java
package com.invoiceapp.controller.mvccontroller;

import com.invoiceapp.dto.misc.RegisterForm;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    //Registration form
    @GetMapping("/register")
    public String registerForm(Model m) {
        m.addAttribute("form", new RegisterForm(null,null,null));
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("form") @Valid RegisterForm form,
                             BindingResult errors,
                             Model m) {

        if (!form.password().equals(form.passwordConfirm()))
            errors.rejectValue("passwordConfirm", null, "Passwords don’t match");

        if (repo.existsByEmail(form.email()))
            errors.rejectValue("email", null, "Email already registered");

        if (errors.hasErrors())
            return "register";

        repo.save(new User(
                null,
                form.email(),
                encoder.encode(form.password()),
                Role.USER,
                true
        ));

        // show login page with message
        m.addAttribute("success","Account created – you can now log in.");
        return "login";
    }

    //Login page
    @GetMapping("/login")
    public String loginPage() { return "login"; }
}
