
package com.invoiceapp; // Ensure correct package

import com.invoiceapp.controller.mvccontroller.InvoiceAdminController;
import com.invoiceapp.dto.client.ClientResponse;
import com.invoiceapp.dto.invoice.InvoiceRequest;
import com.invoiceapp.dto.invoice.InvoiceResponse;
import com.invoiceapp.dto.invoice.RecordPaymentForm;
import com.invoiceapp.entity.Currency;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.User;
import com.invoiceapp.security.DbUserDetailsService;
import com.invoiceapp.security.SecurityConfig; // Import SecurityConfig
import com.invoiceapp.security.UserProvider;
import com.invoiceapp.service.ClientService;
import com.invoiceapp.service.InvoicePdfService;
import com.invoiceapp.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import; // Import annotation
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Import Hamcrest matchers individually if needed for MockMvc assertions, or keep wildcard if heavily used there
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;     // Keep specific argument matchers
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*; // Keep Mockito verify etc.
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceAdminController.class)
@Import(SecurityConfig.class) // <-- ADD THIS IMPORT
public class InvoiceAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // @MockBean deprecation warnings can likely be ignored for now
    @MockitoBean
    private InvoiceService invoiceService;
    @MockitoBean
    private ClientService clientService;
    @MockitoBean
    private InvoicePdfService pdfService;
    @MockitoBean
    private UserProvider userProvider;
    @MockitoBean
    private DbUserDetailsService userDetailsService;

    private InvoiceResponse dummyInvoiceResponse;
    private ClientResponse dummyClientResponse;
    private User dummyUser;


    @BeforeEach
    void setUp() {
        dummyUser = new User(1L, "user@test.com", "pass", com.invoiceapp.entity.Role.USER, true);
        dummyClientResponse = new ClientResponse(1L, "Test Client", "client@test.com", "123");
        dummyInvoiceResponse = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-001")
                .clientId(1L)
                .clientName("Test Client")
                .status(InvoiceStatus.DRAFT)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(10))
                .total(BigDecimal.TEN)
                .currency(Currency.USD)
                .toName("Client Name")
                .fromName("My Name")
                .items(Collections.emptyList()) // <-- FIX: Initialize items list
                .build();
    }

    // --- LIST Tests ---
    @Test
    @WithMockUser
    void listInvoices_NoFilter_ShouldReturnListView() throws Exception {
        Page<InvoiceResponse> page = new PageImpl<>(List.of(dummyInvoiceResponse), PageRequest.of(0, 12), 1);
        given(invoiceService.list(eq(Optional.empty()), eq(0), eq(12))).willReturn(page);
        given(invoiceService.markOverdue()).willReturn(0);

        mockMvc.perform(get("/admin/invoices"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/invoice-list"))
                .andExpect(model().attribute("invoices", hasSize(1)))
                .andExpect(model().attribute("page", notNullValue()));

        verify(invoiceService).markOverdue();
    }

    @Test
    @WithMockUser
    void listInvoices_WithStatusFilter_ShouldReturnFilteredListView() throws Exception {
        Page<InvoiceResponse> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 12), 0);
        given(invoiceService.list(eq(Optional.of(InvoiceStatus.PAID)), eq(0), eq(12))).willReturn(page);
        given(invoiceService.markOverdue()).willReturn(0);


        mockMvc.perform(get("/admin/invoices").param("status", InvoiceStatus.PAID.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/invoice-list"))
                .andExpect(model().attribute("invoices", hasSize(0)))
                .andExpect(model().attribute("filter", InvoiceStatus.PAID));

        verify(invoiceService).markOverdue();
        verify(invoiceService).list(eq(Optional.of(InvoiceStatus.PAID)), eq(0), eq(12));
    }

    // --- CREATE Tests ---
    @Test
    @WithMockUser
    void newInvoiceForm_ShouldReturnFormView() throws Exception {
        given(clientService.findAll()).willReturn(List.of(dummyClientResponse));

        mockMvc.perform(get("/admin/invoices/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/invoice-form"))
                .andExpect(model().attributeExists("form", "clients"));
    }

    @Test
    @WithMockUser
    void submitNewInvoice_ValidData_ShouldRedirectAndAddFlashAttribute() throws Exception {
        given(invoiceService.create(ArgumentMatchers.any(InvoiceRequest.class))).willReturn(dummyInvoiceResponse);

        mockMvc.perform(post("/admin/invoices")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("clientId", "1")
                        .param("dueDate", LocalDate.now().plusDays(10).toString())
                        .param("currency", "USD")
                        .param("toName", "Client A")
                        .param("fromName", "Me")
                        .param("items[0].description", "Item 1")
                        .param("items[0].quantity", "2")
                        .param("items[0].unitPrice", "5.00")
                        .param("bankName",   "Bank")
                        .param("iban",       "GE00INVO0000000000000")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/invoices"))
                .andExpect(flash().attribute("success", containsString("Draft invoice created!")));

        verify(invoiceService).create(ArgumentMatchers.any(InvoiceRequest.class));
    }


    // --- EDIT/UPDATE Tests ---
    @Test
    @WithMockUser
    void editInvoiceForm_ExistingInvoice_ShouldReturnFormViewWithData() throws Exception {
        // This test should now pass because dummyInvoiceResponse.items() is initialized
        given(invoiceService.get(1L)).willReturn(dummyInvoiceResponse);
        given(clientService.findAll()).willReturn(List.of(dummyClientResponse));

        mockMvc.perform(get("/admin/invoices/{id}/edit", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/invoice-form"))
                .andExpect(model().attributeExists("form", "clients"))
                .andExpect(model().attribute("form", hasProperty("clientId", is(1L))));
    }

    @Test
    @WithMockUser
    void updateInvoice_ValidData_ShouldRedirectAndAddFlashAttribute() throws Exception {
        given(invoiceService.update(eq(1L), ArgumentMatchers.any(InvoiceRequest.class))).willReturn(dummyInvoiceResponse);

        mockMvc.perform(post("/admin/invoices/{id}/edit", 1L)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("clientId", "1")
                        .param("dueDate", LocalDate.now().plusDays(15).toString())
                        .param("currency", "USD")
                        .param("toName", "Client Updated")
                        .param("fromName", "Me")
                        .param("items[0].description", "Updated Item")
                        .param("items[0].quantity", "1")
                        .param("items[0].unitPrice", "10.00")
                        .param("bankName",   "Bank")
                        .param("iban",       "GE00INVO0000000000000")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/invoices"))
                .andExpect(flash().attribute("success", containsString("Invoice updated!")));

        verify(invoiceService).update(eq(1L), ArgumentMatchers.any(InvoiceRequest.class));
    }


    // --- ACTION Tests (Send, Mark Paid, Delete, etc.) ---
    @Test
    @WithMockUser
    void sendInvoice_ShouldRedirectAndAddFlashAttribute() throws Exception {
        given(invoiceService.send(1L)).willReturn(dummyInvoiceResponse);

        mockMvc.perform(post("/admin/invoices/{id}/send", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/invoices"))
                .andExpect(flash().attribute("success", containsString("Invoice sent!")));
        verify(invoiceService).send(1L);
    }

    @Test
    @WithMockUser
    void deleteInvoice_ShouldRedirectAndAddFlashAttribute() throws Exception {
        doNothing().when(invoiceService).delete(1L);

        mockMvc.perform(post("/admin/invoices/{id}/delete", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/invoices"))
                .andExpect(flash().attribute("success", containsString("Invoice deleted!")));
        verify(invoiceService).delete(1L);
    }

    @Test
    @WithMockUser
    void recordPaymentForm_ShouldReturnFormView() throws Exception {
        given(invoiceService.get(1L)).willReturn(dummyInvoiceResponse);

        mockMvc.perform(get("/admin/invoices/{id}/record-payment", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/record-payment-form"))
                .andExpect(model().attributeExists("invoice", "form"));
    }


    @Test
    @WithMockUser
    void submitPayment_ValidData_ShouldRedirectAndAddFlashAttribute() throws Exception {
        given(invoiceService.markPaid(eq(1L), ArgumentMatchers.any(RecordPaymentForm.class))).willReturn(dummyInvoiceResponse);

        mockMvc.perform(post("/admin/invoices/{id}/mark-paid", 1L)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("paymentDate", LocalDate.now().toString())
                        .param("paymentMethod", "Bank Transfer")
                        .param("transactionId", "TXN123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/invoices"))
                .andExpect(flash().attribute("success", containsString("Payment recorded")));
        verify(invoiceService).markPaid(eq(1L), ArgumentMatchers.any(RecordPaymentForm.class));
    }


    // --- PDF/Receipt Tests ---
    @Test
    @WithMockUser
    void downloadInvoicePdf_ShouldReturnPdf() throws Exception {
        byte[] pdfBytes = "Fake PDF Content".getBytes();
        given(pdfService.generate(ArgumentMatchers.any(com.invoiceapp.entity.Invoice.class))).willReturn(pdfBytes);
        com.invoiceapp.entity.Invoice dummyEntity = new com.invoiceapp.entity.Invoice();
        given(invoiceService.getEntity(1L)).willReturn(dummyEntity);

        mockMvc.perform(get("/admin/invoices/{id}/pdf", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("invoice-1.pdf")))
                .andExpect(content().bytes(pdfBytes));

        verify(pdfService).generate(ArgumentMatchers.any(com.invoiceapp.entity.Invoice.class));
    }

    @Test
    @WithMockUser
    void downloadReceipt_ShouldReturnPdf() throws Exception {
        byte[] pdfBytes = "Fake Receipt Content".getBytes();
        com.invoiceapp.entity.Invoice invoiceEntity = new com.invoiceapp.entity.Invoice();
        invoiceEntity.setId(1L);
        invoiceEntity.setTransactionId("TXN123");
        given(invoiceService.getEntity(1L)).willReturn(invoiceEntity);
        given(pdfService.generateReceipt(ArgumentMatchers.any(com.invoiceapp.entity.Invoice.class))).willReturn(pdfBytes);

        mockMvc.perform(get("/admin/invoices/{id}/receipt", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("receipt-TXN123.pdf")))
                .andExpect(content().bytes(pdfBytes));

        verify(invoiceService).getEntity(1L);
        verify(pdfService).generateReceipt(ArgumentMatchers.any(com.invoiceapp.entity.Invoice.class));
    }

    // --- Security Tests (Basic Examples) ---
    @Test
    void listInvoices_Unauthenticated_ShouldRedirectToLogin() throws Exception {
        // This test should now pass because SecurityConfig is imported and httpBasic is disabled
        mockMvc.perform(get("/admin/invoices"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}