
package com.invoiceapp;

import com.invoiceapp.controller.mvccontroller.ClientAdminController;
import com.invoiceapp.dto.client.ClientForm;
import com.invoiceapp.dto.client.ClientRequest;
import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.security.DbUserDetailsService;
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientAdminController.class)
public class ClientAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;
    @MockitoBean
    private UserProvider userProvider; // If needed by service indirectly
    @MockitoBean
    private DbUserDetailsService userDetailsService; // For security

    private ClientResponse dummyClientResponse;

    @BeforeEach
    void setUp() {
        dummyClientResponse = new ClientResponse(1L, "Test Client", "client@test.com", "123456");
    }

    // --- LIST Tests ---
    @Test
    @WithMockUser
    void listClients_ShouldReturnListView() throws Exception {
        Page<ClientResponse> page = new PageImpl<>(List.of(dummyClientResponse), PageRequest.of(0, 20), 1);
        given(clientService.list(eq(0), eq(20))).willReturn(page);

        mockMvc.perform(get("/admin/clients"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/client-list"))
                .andExpect(model().attribute("clients", hasSize(1)))
                .andExpect(model().attribute("page", notNullValue()));
    }

    // --- CREATE Tests ---
    @Test
    @WithMockUser
    void newClientForm_ShouldReturnFormView() throws Exception {
        mockMvc.perform(get("/admin/clients/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/client-form"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @WithMockUser
    void submitNewClient_ValidData_ShouldRedirectAndAddFlashAttribute() throws Exception {
        given(clientService.create(any(ClientRequest.class))).willReturn(dummyClientResponse);

        mockMvc.perform(post("/admin/clients")
                        .param("name",        "New Client")
                        .param("email",       "new@client.com")
                        .param("countryCode","+995")
                        .param("rawPhone",    "98765476")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/clients"))
                .andExpect(flash().attribute("success", containsString("Client created!")));

        verify(clientService).create(any(ClientRequest.class));
    }

    // --- EDIT/UPDATE Tests ---
    @Test
    @WithMockUser
    void editClientForm_ExistingClient_ShouldReturnFormViewWithData() throws Exception {
        given(clientService.findById(1L)).willReturn(dummyClientResponse);

        mockMvc.perform(get("/admin/clients/{id}/edit", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/client-form"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("form", hasProperty("id", is(1L))))
                .andExpect(model().attribute("form", hasProperty("name", is("Test Client"))));
    }

    @Test
    @WithMockUser
    void submitUpdateClient_ValidData_ShouldRedirectAndAddFlashAttribute() throws Exception {
        // assume there’s already a client with ID 42
        given(clientService.findById(42L))
                .willReturn(new ClientResponse(42L, "Old Name", "old@example.com", "+995555000"));

        mockMvc.perform(post("/admin/clients")
                        .param("id",           "42")
                        .param("name",         "Updated Name")
                        .param("email",        "updated@example.com")
                        .param("countryCode",  "+995")
                        .param("rawPhone",     "1234567")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Client updated!"))
                .andExpect(redirectedUrl("/admin/clients"));
    }


    // --- DELETE Test ---
    @Test
    @WithMockUser
    void deleteClient_ShouldRedirectAndAddFlashAttribute() throws Exception {
        doNothing().when(clientService).delete(1L);

        mockMvc.perform(post("/admin/clients/{id}/delete", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/clients"))
                .andExpect(flash().attribute("success", containsString("Client deleted!")));
        verify(clientService).delete(1L);
    }


}