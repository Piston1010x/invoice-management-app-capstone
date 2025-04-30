package com.invoiceapp;

import com.invoiceapp.dto.misc.DashboardStats;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.repository.UserRepository;
import com.invoiceapp.security.DbUserDetailsService;
import com.invoiceapp.security.SecurityConfig;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SecurityConfig.class)
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DbUserDetailsService userDetailsService;
    @MockitoBean private DashboardService dashboardService;
    @MockitoBean private UserService userService;
    @MockitoBean private InvoiceService invoiceService;
    @MockitoBean private ClientService clientService;
    @MockitoBean private InvoicePdfService pdfService;
    @MockitoBean private UserProvider userProvider;
    @MockitoBean private EmailService emailService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private InvoiceRepository invoiceRepository;

    @Test
    @WithAnonymousUser
    void testAdminArea_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/invoices"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/clients"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(post("/admin/invoices/1/send").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void testAdminArea_AccessibleWithAuthentication() throws Exception {
        // Mocks for /admin/dashboard
        given(dashboardService.getStatsFor(any(), any(), any())).willReturn(
                new DashboardStats(
                        0L, 0L, 0L, 0L, 0L,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        Map.of(), Map.of()
                )
        );
        given(userService.findByEmail(anyString()))
                .willReturn(new User(1L, "user@example.com", "pass", Role.USER, true));
        given(invoiceService.getRecentInvoices(anyString(), anyInt()))
                .willReturn(Collections.emptyList());
        given(userProvider.getCurrentUser())
                .willReturn(new User(1L, "user@example.com", "pass", Role.USER, true));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());

        // Mocks for /admin/invoices
        given(invoiceService.list(any(), anyInt(), anyInt()))
                .willReturn(Page.empty());
        given(invoiceService.markOverdue())
                .willReturn(0);

        mockMvc.perform(get("/admin/invoices"))
                .andExpect(status().isOk());

        // Mocks for /admin/clients
        given(clientService.list(anyInt(), anyInt()))
                .willReturn(Page.empty());

        mockMvc.perform(get("/admin/clients"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testApiArea_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void testApiArea_AccessibleWithAuthentication() throws Exception {
        given(invoiceService.list(any(), anyInt(), anyInt()))
                .willReturn(Page.empty());

        given(clientService.findAll())
                .willReturn(Collections.emptyList());

        given(userRepository.findByEmail(anyString()))
                .willReturn(Optional.of(new User(1L, "user@test.com", "pass", Role.USER, true)));

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk());
    }
}
