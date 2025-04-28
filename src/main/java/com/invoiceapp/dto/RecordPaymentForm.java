package com.invoiceapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecordPaymentForm {
    @NotNull
    private LocalDate paymentDate;

    @NotBlank
    private String paymentMethod;

    private String paymentNotes;

    @NotBlank
    private String transactionId;
}
