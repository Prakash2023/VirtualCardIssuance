package com.example.virtualCard.services;

import com.example.virtualCard.entity.Card;
import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.enums.TransactionStatus;
import com.example.virtualCard.enums.TransactionType;
import com.example.virtualCard.exception.CardNotFoundException;
import com.example.virtualCard.exception.IdempotencyConflictException;
import com.example.virtualCard.exception.IdempotencyInProgressException;
import com.example.virtualCard.exception.InsufficientBalanceException;
import com.example.virtualCard.repository.CardRepository;
import com.example.virtualCard.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public class IdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public IdempotencyService(CardRepository cardRepository, TransactionRepository transactionRepository) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    public Transaction reserveIdempotencyKey(Card card, TransactionType type, BigDecimal amount, String idempotencyKey) {
        try {
            Transaction pending = new Transaction(card, type, amount, TransactionStatus.PENDING, idempotencyKey);
            return transactionRepository.saveAndFlush(pending);
        } catch (DataIntegrityViolationException ex) {
            Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
            UUID expectedCardId = TransactionType.ISSUANCE.equals(type) ? null : card.getId();
            validateIdempotentReplay(existing, expectedCardId, type, amount);
            if (existing.getStatus() == TransactionStatus.PENDING) {
                throw new IdempotencyInProgressException();
            }
            return existing;
        }
    }

    public Card replayCreate(Transaction existing, String expectedName, BigDecimal expectedAmount) {
        validateIdempotentReplay(existing, null, TransactionType.ISSUANCE, expectedAmount);
        if (existing.getStatus() == TransactionStatus.PENDING) {
            throw new IdempotencyInProgressException();
        }
        log.info("Idempotent replay for issuance idempotencyKey={}", existing.getIdempotencyKey());
        Card existingCard = getCard(existing.getCard().getId());
        if (!Objects.equals(existingCard.getCardholderName(), expectedName)) {
            throw new IdempotencyConflictException("Idempotency key reused with different request payload");
        }
        return existingCard;
    }

    public Card replayTopup(Transaction existing, UUID cardId, BigDecimal amount) {
        validateIdempotentReplay(existing, cardId, TransactionType.TOPUP, amount);
        if (existing.getStatus() == TransactionStatus.PENDING) {
            throw new IdempotencyInProgressException();
        }
        log.info("Idempotent replay for topup cardId={} idempotencyKey={}", cardId, existing.getIdempotencyKey());
        return getCard(cardId);
    }

    public Card replaySpend(Transaction existing, UUID cardId, BigDecimal amount) {
        validateIdempotentReplay(existing, cardId, TransactionType.SPEND, amount);
        if (existing.getStatus() == TransactionStatus.PENDING) {
            throw new IdempotencyInProgressException();
        }
        log.info("Idempotent replay for spend cardId={} idempotencyKey={}", cardId, existing.getIdempotencyKey());
        if (existing.getStatus() == TransactionStatus.DECLINED) {
            throw new InsufficientBalanceException();
        }
        return getCard(cardId);
    }

    public void validateIdempotentReplay(Transaction existing, UUID expectedCardId, TransactionType expectedType, BigDecimal expectedAmount) {
        if (!expectedType.equals(existing.getType())
                || (expectedCardId != null && !Objects.equals(expectedCardId, existing.getCard().getId()))
                || (expectedAmount != null && existing.getAmount().compareTo(expectedAmount) != 0)) {
            throw new IdempotencyConflictException("Idempotency key reused with different request payload");
        }
    }

    private Card getCard(UUID id) {
        return cardRepository.findById(id).orElseThrow(CardNotFoundException::new);
    }
}
