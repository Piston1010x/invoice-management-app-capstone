package com.invoiceapp.exception;

//Custom exception in case a client tries deleting a client with active(sent/paid) invoices
public class ClientHasActiveInvoicesException extends RuntimeException {
    private final long count;
    public ClientHasActiveInvoicesException(long count) {
        super("Client has active invoices");
        this.count = count;
    }
    public long getCount() { return count; }
}
