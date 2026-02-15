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

    @Column(nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private CardStatus cardStatus;

    private LocalDateTime createdAt;
    public Card() {}

    public Card(String cardHolderName, BigDecimal balance) {
        this.cardholderName = cardHolderName;
        this.balance = balance;
        this.cardStatus = CardStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
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

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public CardStatus getStatus() {
        return cardStatus;
    }

    public void setStatus(CardStatus status) {
        this.cardStatus = status;
    }

}
