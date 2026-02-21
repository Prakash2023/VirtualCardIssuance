package com.example.virtualCard.dto;

import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.enums.TransactionStatus;
import com.example.virtualCard.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionResponse {
    private final UUID id;
    private final UUID cardId;
    private final BigDecimal amount;
    private final TransactionType type;
    private final String idempotencyKey;
    private final TransactionStatus status;
    private final LocalDateTime createdAt;

    public TransactionResponse(
            UUID id,
            UUID cardId,
            BigDecimal amount,
            TransactionType type,
            String idempotencyKey,
            TransactionStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.cardId = cardId;
        this.amount = amount;
        this.type = type;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCard().getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getIdempotencyKey(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getCardId() {
        return cardId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
