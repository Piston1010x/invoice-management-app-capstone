package com.invoiceapp.security;

import com.invoiceapp.entity.User;
import com.invoiceapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProvider {
    private final UserRepository userRepo;


    //retrieve the current logged in user, used during program runtime
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Retrieving user with email: {}", email);
        return userRepo.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });
    }
}
