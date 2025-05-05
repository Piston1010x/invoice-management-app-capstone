// src/main/java/com/invoiceapp/security/DbUserDetailsService.java
package com.invoiceapp.security;

import com.invoiceapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Slf4j
@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    //used once during login to find user in db
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Attempting to get user details for login with email: {}", email);
        return repo.findByEmail(email)
                .map(AppUserDetails::new)
                .orElseThrow(() -> {
                            log.error("User not found with email: {}", email);
                            return new UsernameNotFoundException("User not found: " + email);
                        });
        }
    }
