package com.invoiceapp.dto.invoice;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

//mark paid form

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
