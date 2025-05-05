package com.invoiceapp.security;

import com.invoiceapp.security.DbUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final DbUserDetailsService uds;

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {
        log.info("Applying security configuration...");
        http.csrf(csrf -> csrf
                        //store token in a cookie
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**",
                                "/login", "/register",
                                "/public/**", "/error"
                        ).permitAll()
                        .requestMatchers("/admin/**", "/api/v1/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout

                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .userDetailsService(uds)
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(new AccessDeniedHandler() {
                            @Override
                            public void handle(
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    AccessDeniedException denied
                            ) throws IOException, ServletException {
                                request.getSession().setAttribute(
                                        "error",
                                        "You don’t have permission to perform that action."
                                );
                                request.getSession().setAttribute(
                                        "error",
                                        "You don’t have permission to perform that action."
                                );


                                String referer = request.getHeader("Referer");
                                response.sendRedirect(
                                        (referer != null && !referer.isEmpty())
                                                ? referer
                                                : "/admin/dashboard"
                                );
                            }
                        })
                );
        log.info("Security configuration applied successfully.");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Creating BCryptPasswordEncoder bean for password encoding.");
        return new BCryptPasswordEncoder();
    }
}