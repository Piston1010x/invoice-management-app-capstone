package com.invoiceapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Invoice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true , unique = true, length = 30)
    private String invoiceNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Client client;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    private LocalDate issueDate = LocalDate.now();
    private LocalDate dueDate;

    @Column(name = "payment_token", unique = true, length = 60)
    private String paymentToken;                    // set on send()

    @Column(name = "payment_intent_time")
    private LocalDateTime paymentIntentAt;          // set when client clicks link


    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    public BigDecimal getTotal() {
        return items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
