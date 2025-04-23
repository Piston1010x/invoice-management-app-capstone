package com.invoiceapp.entity;

public enum InvoiceStatus {
    DRAFT,
    SENT,
    PAID,
    OVERDUE;

    public boolean canTransitionTo(InvoiceStatus next) {
        return switch (this) {
            case DRAFT -> next == SENT;
            case SENT, OVERDUE -> next == PAID;
            case PAID -> false;
        };
    }
}
