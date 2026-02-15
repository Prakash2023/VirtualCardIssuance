package com.example.virtualCard.dto;

import java.math.BigDecimal;

public class CreateCardRequest {
    private String cardholderName;
    private BigDecimal initialBalance;

    public String getCardholderName() { return cardholderName; }
    public BigDecimal getInitialBalance() { return initialBalance; }
}
