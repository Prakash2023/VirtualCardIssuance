package com.example.virtualCard.exception;

public class CardNotFoundException extends RuntimeException{
    public CardNotFoundException(){
        super("Card not found");
    }
}
