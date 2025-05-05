package com.invoiceapp.entity;


//invoice statuses
public enum InvoiceStatus {
    DRAFT,
    SENT,
    PAID,
    OVERDUE;

    //method for transitions
    public boolean canTransitionTo(InvoiceStatus next) {
        return switch (this) {
            case DRAFT -> next == SENT;
            case SENT, OVERDUE -> next == PAID;
            case PAID -> false;
        };
    }
}
