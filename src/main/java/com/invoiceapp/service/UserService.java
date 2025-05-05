package com.invoiceapp.service;

import com.invoiceapp.dto.misc.UserResponse;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //Method to find user by email
    public User findByEmail(String email) {
        log.info("Attempting to find user with email: {}", email);
        return userRepository.findByEmail(email)
                .map(obj -> obj)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new IllegalArgumentException("User not found: " + email);
                });
    }

    // For admin view as dropdown
    public List<User> findAllUsers() {
        List<User> users = userRepository.findAll();
        log.info("Fetched {} users from the database", users.size());
        return users;
    }


    // Lookup a user by ID (for “view as”)
    public User findById(Long id) {
        log.info("Attempting to find user with ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new IllegalArgumentException("User not found: " + id);
                });
    }




    //method to display user list for admin
    @Transactional
    public Page<UserResponse> listForAdmin(int page, int size) {
        log.info("Fetching user list for admin: page={}, size={}", page, size);
        Pageable pg = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<UserResponse> result = userRepository.findAll(pg)
                .map(user -> new UserResponse(user.getId(), user.getEmail(), user.getRole()));
        log.info("Fetched {} users for admin view", result.getTotalElements());
        return result;
    }


    //method to promote to admin
    @Transactional
    public void promoteToAdmin(Long id) {
        log.info("Promoting user with ID: {} to ADMIN", id);
        User u = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new IllegalArgumentException("User not found: " + id);
                });
        u.setRole(Role.ADMIN);
        userRepository.save(u);
        log.info("User with ID: {} successfully promoted to ADMIN", id);
    }


    //demote from admin
    @Transactional
    public void demoteToUser(Long id) {
        log.info("Demoting user with ID: {} to USER", id);
        User u = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new EntityNotFoundException("User not found");
                });
        u.setRole(Role.USER);
        userRepository.save(u);
        log.info("User with ID: {} successfully demoted to USER", id);
    }


    //change password
    @Transactional
    public void changePassword(String username, String oldRawPassword, String newRawPassword) {
        log.info("Changing password for user: {}", username);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", username);
                    return new UsernameNotFoundException(username);
                });

        if (!passwordEncoder.matches(oldRawPassword, user.getPasswordHash())) {
            log.error("Password mismatch for user: {}", username);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        log.info("Password successfully changed for user: {}", username);
    }
}
