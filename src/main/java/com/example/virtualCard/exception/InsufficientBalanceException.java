package com.example.virtualCard.exception;

public class InsufficientBalanceException extends RuntimeException{
    public InsufficientBalanceException() {
        super("Insufficient balance");
    }
}
