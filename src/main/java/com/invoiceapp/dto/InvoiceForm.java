package com.invoiceapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceForm {
    private Long clientId;
    private String desc1;
    private Integer qty1;
    private BigDecimal price1;
    private String desc2;
    private Integer qty2;
    private BigDecimal price2;
    private String desc3;
    private Integer qty3;
    private BigDecimal price3;
}
