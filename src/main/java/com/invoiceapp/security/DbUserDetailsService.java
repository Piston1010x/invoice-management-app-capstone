// src/main/java/com/invoiceapp/security/DbUserDetailsService.java
package com.invoiceapp.security;

import com.invoiceapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repo.findByEmail(email)
                .map(AppUserDetails::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + email));
    }
}
