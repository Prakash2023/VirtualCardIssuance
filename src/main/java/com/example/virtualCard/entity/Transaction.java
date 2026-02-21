package com.example.virtualCard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    private BigDecimal amount;
    private String type;  // spend or topup

    @Column(unique = true, length = 100,nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private LocalDateTime createdAt;
    public Transaction(){}
    public Transaction(Card card, String type, BigDecimal amount, TransactionStatus status, String idempotencyKey) {
        this.card = card;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Card getCard() { return card; }
    public void setCard(Card card) { this.card = card; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
