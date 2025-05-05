package com.invoiceapp;

import com.invoiceapp.controller.AdminHomeController;
import com.invoiceapp.dto.invoice.InvoiceResponse;
import com.invoiceapp.dto.misc.DashboardStats;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.DashboardService;
import com.invoiceapp.service.InvoiceService;
import com.invoiceapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminHomeController.class)  // Use WebMvcTest for controller tests
class AdminHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserProvider userProvider;

    @InjectMocks
    private AdminHomeController adminHomeController;

    private User mockUser;
    private UserDetails mockUserDetails;
    private DashboardStats mockDashboardStats;
    private List<InvoiceResponse> mockInvoices;

    @BeforeEach
    void setup() {
        mockUser = new User();
        mockUser.setEmail("admin@example.com");
        mockUser.setRole(Role.ADMIN);

        mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("admin@example.com");

        mockDashboardStats = mock(DashboardStats.class);
        mockInvoices = List.of(mock(InvoiceResponse.class), mock(InvoiceResponse.class));

        // Mocking UserService
        when(userService.findByEmail("admin@example.com")).thenReturn(mockUser);
        when(userService.findByEmail(anyString())).thenReturn(mockUser);
    }

    //Test redirection from root to dashboard
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})  // Mock authentication
    void testRootRedirect() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }
}
