package com.example.virtualCard.exception;

public class IdempotencyInProgressException extends RuntimeException {
    public IdempotencyInProgressException() {
        super("A request with this idempotency key is already in progress");
    }
}
