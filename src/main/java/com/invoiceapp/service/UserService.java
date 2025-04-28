package com.invoiceapp.service;

import com.invoiceapp.entity.User;
import com.invoiceapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;

    public User findByEmail(String email) {
        return repo.findByEmail(email)
                .map(obj -> obj) // Only if your current findByEmail returns Optional<Object>
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }
}
