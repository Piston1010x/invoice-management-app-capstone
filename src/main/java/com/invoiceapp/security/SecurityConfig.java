package com.invoiceapp.security;

import com.invoiceapp.security.DbUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // Import for disabling
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// src/main/java/com/invoiceapp/security/SecurityConfig.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final DbUserDetailsService uds;

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http
                // Explicitly disable CSRF if you don't need it (common for APIs, but be cautious)
                // If your Thymeleaf forms rely on it, keep it enabled or configure appropriately.
                .csrf(AbstractHttpConfigurer::disable) // Keep if already disabled and intended

                // Explicitly disable HTTP Basic authentication
                .httpBasic(AbstractHttpConfigurer::disable) // <-- ADD THIS LINE

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", // Static resources
                                "/login", "/register",             // Auth pages
                                "/public/**", "/error"            // Public endpoints
                        ).permitAll() // Publicly accessible
                        .requestMatchers("/admin/**", "/api/v1/**").authenticated() // Secure admin and API areas
                        .anyRequest().authenticated() // Secure everything else by default
                )
                .formLogin(form -> form
                        .loginPage("/login") // Specify the login page URL
                        .defaultSuccessUrl("/admin/dashboard", true) // Redirect on success
                        .permitAll() // Allow access to the login page itself
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout") // Redirect after logout
                        .permitAll() // Allow access to logout functionality
                )
                .userDetailsService(uds); // Use custom UserDetailsService

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}