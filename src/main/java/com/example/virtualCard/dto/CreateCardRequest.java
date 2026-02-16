package com.example.virtualCard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class CreateCardRequest {
    @NotBlank(message = "cardholderName is required")
    private String cardholderName;

    @NotNull(message = "initialBalance is required")
    @PositiveOrZero(message = "initialBalance cannot be negative")
    private BigDecimal initialBalance;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    public String getCardholderName() { return cardholderName; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
