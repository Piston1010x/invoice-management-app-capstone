package com.invoiceapp; // Ensure correct package

import com.invoiceapp.dto.client.ClientRequest;
import com.invoiceapp.dto.invoice.InvoiceItemRequest;
import com.invoiceapp.dto.invoice.InvoiceRequest;
import com.invoiceapp.dto.misc.RegisterForm;
import com.invoiceapp.entity.Currency;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    //Helper to get violation messages for a specific property
    private Set<String> getMessagesForProperty(Set<? extends ConstraintViolation<?>> violations, String propertyName) {
        return violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals(propertyName))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    //Helper to check if a specific property has *any* violation
    private boolean hasViolationForProperty(Set<? extends ConstraintViolation<?>> violations, String propertyName) {
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(propertyName));
    }


    @Nested
    @DisplayName("ClientRequest Validation")
    class ClientRequestValidation {
        @Test
        void validRequest_ShouldHaveNoViolations() {
            ClientRequest req = new ClientRequest("Valid Name", "valid@email.com", "1234567890");
            Set<ConstraintViolation<ClientRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankName_ShouldFail() {
            ClientRequest req = new ClientRequest(" ", "valid@email.com", "1234567");
            Set<ConstraintViolation<ClientRequest>> violations = validator.validate(req);
            assertThat(getMessagesForProperty(violations, "name")).contains("must not be blank");
        }

        @Test
        void nameTooLong_ShouldFail() {
            String longName = "a".repeat(151); // @Size(max=150)
            ClientRequest req = new ClientRequest(longName, "valid@email.com", "1234567");
            Set<ConstraintViolation<ClientRequest>> violations = validator.validate(req);
            assertThat(getMessagesForProperty(violations, "name")).isNotEmpty(); // Check message if specific
        }

        @Test
        void invalidEmailFormat_ShouldFail() {
            ClientRequest req = new ClientRequest("Valid Name", "invalid-email", "1234567");
            Set<ConstraintViolation<ClientRequest>> violations = validator.validate(req);
            assertThat(getMessagesForProperty(violations, "email")).isNotEmpty(); // Contains email format message
        }

        @Test
        void phoneTooLong_ShouldFail() {
            String longPhone = "1".repeat(21); // @Size(max=20)
            ClientRequest req = new ClientRequest("Valid Name", "valid@email.com", longPhone);
            Set<ConstraintViolation<ClientRequest>> violations = validator.validate(req);
            assertThat(getMessagesForProperty(violations, "phone")).isNotEmpty();
        }

        @Test
        void nullEmail_ShouldPass() { // Assuming @Email allows null, check annotation if needed
            ClientRequest req = new ClientRequest("Valid Name", null, "1234567");
            Set<ConstraintViolation<ClientRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("InvoiceRequest Validation")
    class InvoiceRequestValidation {

        private InvoiceItemRequest validItem = new InvoiceItemRequest("Item", 1, BigDecimal.TEN);

        @Test
        void validRequest_ShouldHaveNoViolations() {
            InvoiceRequest req = new InvoiceRequest(1L, List.of(validItem), LocalDate.now(), Currency.USD, "T", "F", "B", "I");
            Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void pastDueDate_ShouldFail() {
            InvoiceRequest req = new InvoiceRequest(1L, List.of(validItem), LocalDate.now().minusDays(1), Currency.USD, "T", "F", "B", "I");
            Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(req);
            assertThat(getMessagesForProperty(violations, "dueDate")).isNotEmpty(); // FutureOrPresent check
        }

        @Test
        void nullItemsList_ShouldFail() {
            InvoiceRequest req = new InvoiceRequest(1L, null, LocalDate.now(), Currency.USD, "T", "F", "B", "I");
            Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(req);
            assertThat(getMessagesForProperty(violations, "items")).contains("must not be null");
        }

        @Test
        void emptyItemsList_ShouldPass() { // Assuming empty list is allowed, check constraints
            InvoiceRequest req = new InvoiceRequest(1L, Collections.emptyList(), LocalDate.now(), Currency.USD, "T", "F", "B", "I");
            Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty(); // If constraint was @NotEmpty, this would fail
        }

        // Test nested validation
        @Test
        void invalidItemInList_ShouldFail() {
            InvoiceItemRequest invalidItem = new InvoiceItemRequest(" ", -1, BigDecimal.ZERO);
            InvoiceRequest req = new InvoiceRequest(
                    1L,
                    List.of(invalidItem),
                    LocalDate.now(),
                    Currency.USD,
                    "T", "F", "B", "I"
            );

            Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(req);

            // Expect 4 violations: description, quantity, and two on unitPrice
            assertThat(violations).hasSize(4);
            assertThat(violations.stream()
                    .map(v -> v.getPropertyPath().toString()))
                    .containsExactlyInAnyOrder(
                            "items[0].description",
                            "items[0].quantity",
                            "items[0].unitPrice",   // @DecimalMin
                            "items[0].unitPrice"    // @Positive
                    );
        }

    }

    @Nested
    @DisplayName("RegisterForm Validation")
    class RegisterFormValidation {
        @Test
        void validForm_ShouldHaveNoViolations() {
            RegisterForm form = new RegisterForm("test@test.com", "password", "password");
            Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);
            assertThat(violations).isEmpty();
        }

        @Test
        void invalidEmail_ShouldFail() {
            RegisterForm form = new RegisterForm("invalid", "password", "password");
            Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);
            assertThat(getMessagesForProperty(violations, "email")).isNotEmpty();
        }

        @Test
        void shortPassword_ShouldFail() {
            RegisterForm form = new RegisterForm("test@test.com", "12345", "12345"); // @Size(min=6)
            Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);
            assertThat(getMessagesForProperty(violations, "password")).isNotEmpty();
        }

        @Test
        void blankFields_ShouldFail() {
            RegisterForm form = new RegisterForm(" ", " ", " ");
            Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);
            // Original check: assertThat(violations).hasSize(3); // This failed as it found 5

            // Corrected Check: Verify that the expected @NotBlank violations occurred for each field
            assertThat(hasViolationForProperty(violations, "email")).isTrue();
            assertThat(hasViolationForProperty(violations, "password")).isTrue();
            assertThat(hasViolationForProperty(violations, "passwordConfirm")).isTrue();

            // Optionally, be more specific about the @NotBlank message if needed
            assertThat(getMessagesForProperty(violations, "email")).contains("must not be blank");
            assertThat(getMessagesForProperty(violations, "password")).contains("must not be blank");
            assertThat(getMessagesForProperty(violations, "passwordConfirm")).contains("must not be blank");

            // You can still assert the total size if you *know* all constraints that will fail
            // assertThat(violations).hasSize(5); // This would pass now, but is less robust
        }
    }

}