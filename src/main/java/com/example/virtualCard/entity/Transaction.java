package com.example.virtualCard.entity;

import com.example.virtualCard.enums.TransactionStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")

public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID cardId;
    private BigDecimal amount;
    private String type;  // spend or topup
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private LocalDateTime createdAt;
    public Transaction(){}
    public Transaction(UUID cardId, String type, BigDecimal amount, TransactionStatus status) {
        this.cardId = cardId;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCardId() { return cardId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
