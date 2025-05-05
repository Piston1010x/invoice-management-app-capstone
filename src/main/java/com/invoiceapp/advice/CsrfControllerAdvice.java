package com.invoiceapp.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

@Slf4j
@ControllerAdvice
public class CsrfControllerAdvice {
    @ModelAttribute
    public void addCsrfToken(Model model, HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        model.addAttribute("_csrf", token);
        log.debug("CSRF token added to model: {}", token.getToken());

    }
}
