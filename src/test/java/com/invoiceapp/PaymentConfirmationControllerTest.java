package com.invoiceapp;

import com.invoiceapp.controller.PaymentConfirmationController;
import com.invoiceapp.entity.Client;
import com.invoiceapp.entity.Invoice;
import com.invoiceapp.entity.InvoiceStatus;
import com.invoiceapp.entity.Role;
import com.invoiceapp.entity.User;
import com.invoiceapp.repository.InvoiceRepository;
import com.invoiceapp.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentConfirmationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentConfirmationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceRepository invoiceRepository;
    @MockitoBean
    private EmailService emailService;

    private Invoice testInvoice;
    private String validToken;
    private String invalidToken;

    @BeforeEach
    void setUp() {
        validToken = UUID.randomUUID().toString();
        invalidToken = UUID.randomUUID().toString();

        User testUser = new User(1L, "owner@test.com", "pass", Role.USER, true);
        Client testClient = new Client(1L, "Test Client", "client@test.com", "123", testUser);

        testInvoice = new Invoice();
        testInvoice.setId(1L);
        testInvoice.setPaymentToken(validToken);
        testInvoice.setInvoiceNumber("INV-123");
        testInvoice.setClient(testClient);
        testInvoice.setUser(testUser);
        testInvoice.setStatus(InvoiceStatus.SENT);
        testInvoice.setPaymentIntentAt(null);

        given(invoiceRepository.findByPaymentToken(validToken)).willReturn(Optional.of(testInvoice));
        given(invoiceRepository.findByPaymentToken(invalidToken)).willReturn(Optional.empty());
    }

    @Test
    void confirm_validTokenAlreadyConfirmed_shouldReturnOkWithoutAction() throws Exception {
        CsrfToken dummyToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "dummy");

        // Simulate the invoice already having been paid
        testInvoice.setPaymentIntentAt(LocalDateTime.now().minusHours(1));
        given(invoiceRepository.findByPaymentToken(validToken)).willReturn(Optional.of(testInvoice));

        // Perform the request
        mockMvc.perform(get("/public/confirm-payment/{token}", validToken)
                        .requestAttr(CsrfToken.class.getName(), dummyToken))
                .andExpect(status().isOk())
                .andExpect(content().string(equalToIgnoringWhiteSpace(
                        "We already recorded your payment. Thank you!"
                )));

        // Verify no email was sent
        verify(emailService, never()).simpleNotify(any(), any(), any());
    }

    @Test
    void confirm_invalidToken_shouldReturnNotFound() throws Exception {
        CsrfToken dummyToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "dummy");

        // Perform the request with an invalid token
        mockMvc.perform(get("/public/confirm-payment/{token}", invalidToken)
                        .requestAttr(CsrfToken.class.getName(), dummyToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string(equalToIgnoringWhiteSpace(
                        "Invalid or expired link."
                )));

        // Verify no email was sent
        verify(emailService, never()).simpleNotify(any(), any(), any());
    }
}
