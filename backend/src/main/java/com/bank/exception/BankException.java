package com.bank.exception;

import org.springframework.http.HttpStatus;

public class BankException extends RuntimeException {
    private final HttpStatus status;
    public BankException(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
