package com.example.virtualCard.exception;

public class CardNotActiveException extends RuntimeException{
    public CardNotActiveException(){
        super("Card is not active");
    }
}
