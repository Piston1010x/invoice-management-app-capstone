package com.invoiceapp.exception;

// src/main/java/com/invoiceapp/exception/ClientHasActiveInvoicesException.java
public class ClientHasActiveInvoicesException extends RuntimeException {
    private final long count;
    public ClientHasActiveInvoicesException(long count) {
        super("Client has active invoices");
        this.count = count;
    }
    public long getCount() { return count; }
}
