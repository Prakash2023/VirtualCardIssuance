package com.example.virtualCard.entity;

import com.example.virtualCard.enums.CardStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards")

public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String cardholderName;

    @Column(nullable = false, columnDefinition = "NUMERIC CHECK (balance >= 0)")
    private BigDecimal balance;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private CardStatus cardStatus;

    private LocalDateTime createdAt;
    public Card() {}

    public Card(String cardHolderName, BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("initialBalance cannot be negative");
        }
        this.cardholderName = cardHolderName;
        this.balance = balance;
        this.cardStatus = CardStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }
    public void debit(BigDecimal amount) {
        validateMonetaryAmount(amount);

        if (cardStatus != CardStatus.ACTIVE)
            throw new IllegalStateException("Card inactive");

        if (balance.compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient balance");

        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        validateMonetaryAmount(amount);

        if (cardStatus != CardStatus.ACTIVE)
            throw new IllegalStateException("Card inactive");

        balance = balance.add(amount);
    }
    public UUID getId() {
        return id;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardHolderName) {
        this.cardholderName = cardHolderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public CardStatus getStatus() {
        return cardStatus;
    }

    public void setStatus(CardStatus status) {
        this.cardStatus = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID cardId) {
        this.id=cardId;
    }

    private void validateMonetaryAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }
}
