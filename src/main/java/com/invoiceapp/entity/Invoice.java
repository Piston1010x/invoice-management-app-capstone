package com.invoiceapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;
    @Column(nullable = true , unique = true, length = 30)
    private String invoiceNumber;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Client client;
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.DRAFT;
    private LocalDate issueDate = LocalDate.now();
    private LocalDate dueDate;
    @Column(name = "payment_token", unique = true, length = 60)
    private String paymentToken;
    @Column(nullable = false)
    private boolean archived = false;
    @Column(name = "payment_intent_time")
    private LocalDateTime paymentIntentAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    @Column(name = "payment_method")
    private String paymentMethod;
    @Column(name = "payment_amount_recorded")
    private BigDecimal paymentAmountRecorded;
    @Column(name = "payment_notes")
    private String paymentNotes;


    @Column(name = "to_email")
    private String to;
    @Column(name = "to_name")
    private String toName;

    @Column(name = "from_email")
    private String from;
    @Column(name = "from_name")
    private String fromName;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currency;
    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "iban")
    private String iban;

    @Column(nullable = true)   // allow DB NULL
    private String transactionId;      // <-- new

    /**
     * Before inserting or updating, recalculate the 'total' field
     * so that it's always the sum of item.amount.
     */
    @PrePersist
    @PreUpdate
    private void recalcTotal() {
        this.total = items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Convenience getter that also sums items on the fly.
     */
    public BigDecimal getTotal() {
        return items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
