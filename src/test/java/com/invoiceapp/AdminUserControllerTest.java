package com.invoiceapp;

import com.invoiceapp.controller.AdminUserController;
import com.invoiceapp.dto.misc.UserResponse;
import com.invoiceapp.entity.Role;
import com.invoiceapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Autowire MockMvc

    @MockitoBean
    private UserService userService;  // Mock the UserService here

    @InjectMocks
    private AdminUserController adminUserController;  // Inject controller

    @BeforeEach
    void setUp() {}

    @Test
    @WithMockUser(roles = "ADMIN")
    void testListUsers() throws Exception {
        //Mock the userService to return a page of users with roles
        List<UserResponse> userResponses = Arrays.asList(
                new UserResponse(1L, "user1@example.com", Role.USER),
                new UserResponse(2L, "user2@example.com", Role.ADMIN)
        );
        Page<UserResponse> page = new PageImpl<>(userResponses);

        when(userService.listForAdmin(0, 10)).thenReturn(page);

        // Act & Assert: Perform a GET request and check if the response contains the users
        mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-list"))
                .andExpect(model().attribute("page", page))
                .andExpect(model().attribute("users", userResponses));

        verify(userService, times(1)).listForAdmin(0, 10);
    }
}
