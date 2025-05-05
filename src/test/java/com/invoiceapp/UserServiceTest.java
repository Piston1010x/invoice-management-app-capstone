package com.invoiceapp;

import com.invoiceapp.dto.misc.UserResponse;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.UserRepository;
import com.invoiceapp.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "user@example.com", "hashedpass", Role.USER, true);
    }

    // --- findByEmail Tests ---
    @Test
    void findByEmail_ExistingUser_ShouldReturnUser() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));

        User result = userService.findByEmail("user@example.com");

        assertThat(result).isEqualTo(user);
        verify(userRepository).findByEmail("user@example.com");
    }

    @Test
    void findByEmail_UserNotFound_ShouldThrowException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("nonexistent@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: nonexistent@example.com");
    }

    // --- findAllUsers Tests ---
    @Test
    void findAllUsers_ShouldReturnListOfUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> users = userService.findAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0)).isEqualTo(user);
        verify(userRepository).findAll();
    }

    // --- findById Tests ---
    @Test
    void findById_ExistingUser_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        User result = userService.findById(1L);

        assertThat(result).isEqualTo(user);
        verify(userRepository).findById(1L);
    }

    @Test
    void findById_UserNotFound_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: 999");
    }

    // --- listForAdmin Tests ---
    @Test
    void listForAdmin_ShouldReturnPagedUsers() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<User> usersPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(usersPage);

        Page<UserResponse> result = userService.listForAdmin(0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("user@example.com");
        verify(userRepository).findAll(pageable);
    }

    // --- promoteToAdmin Tests ---
    @Test
    void promoteToAdmin_ShouldChangeRoleToAdmin() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        userService.promoteToAdmin(1L);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void promoteToAdmin_UserNotFound_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.promoteToAdmin(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: 999");
    }

    // --- demoteToUser Tests ---
    @Test
    void demoteToUser_ShouldChangeRoleToUser() {
        user.setRole(Role.ADMIN); // Ensure user is an admin before demotion
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        userService.demoteToUser(1L);

        assertThat(user.getRole()).isEqualTo(Role.USER);
        verify(userRepository).save(user);
    }

    @Test
    void demoteToUser_UserNotFound_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.demoteToUser(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // --- changePassword Tests ---
    @Test
    void changePassword_ValidOldPassword_ShouldChangePassword() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("oldpassword", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("newhashedpass");

        userService.changePassword("user@example.com", "oldpassword", "newpassword");

        assertThat(user.getPasswordHash()).isEqualTo("newhashedpass");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_InvalidOldPassword_ShouldThrowException() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("user@example.com", "wrongpassword", "newpassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void changePassword_UserNotFound_ShouldThrowException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.changePassword("nonexistent@example.com", "oldpassword", "newpassword"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nonexistent@example.com");
    }
}
