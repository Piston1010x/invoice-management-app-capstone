package com.invoiceapp.exception;


//exception for when client has active invoices and a user tries to delete them
public class ClientHasActiveInvoicesException extends RuntimeException {
    private final long count;
    public ClientHasActiveInvoicesException(long count) {
        super("Client has active invoices");
        this.count = count;
    }


    public long getCount() {
        return count;
    }
}
