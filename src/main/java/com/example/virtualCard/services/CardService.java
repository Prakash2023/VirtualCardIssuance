package com.example.virtualCard.services;

import com.example.virtualCard.dto.TransactionResponse;
import com.example.virtualCard.entity.Card;
import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.enums.CardStatus;
import com.example.virtualCard.enums.TransactionStatus;
import com.example.virtualCard.exception.CardNotActiveException;
import com.example.virtualCard.exception.CardNotFoundException;
import com.example.virtualCard.exception.IdempotencyConflictException;
import com.example.virtualCard.exception.InsufficientBalanceException;
import com.example.virtualCard.repository.CardRepository;
import com.example.virtualCard.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CardService {
    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private static final String TYPE_ISSUANCE = "ISSUANCE";
    private static final String TYPE_TOPUP = "TOPUP";
    private static final String TYPE_SPEND = "SPEND";

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public CardService(
            CardRepository cardRepository,
            TransactionRepository transactionRepository
    ) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Card createCard(String name, BigDecimal amount, String idempotencyKey) {
        requireNonNegativeAmount(amount, "initialBalance");
        Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return replayCreate(existing, name, amount);
        }

        Card card = cardRepository.save(new Card(name, amount));
        Transaction issuance = reserveIdempotencyKey(card, TYPE_ISSUANCE, amount, idempotencyKey);

        if (!Objects.equals(issuance.getCard().getId(), card.getId())) {
            cardRepository.delete(card);
            return replayCreate(issuance, name, amount);
        }

        issuance.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(issuance);

        log.info("Issued card {} with idempotencyKey={}", card.getId(), idempotencyKey);
        return card;
    }

    public Card getCard(UUID id) {
        return cardRepository.findById(id).orElseThrow(CardNotFoundException::new);
    }

    @Transactional
    public Card topup(UUID cardId, BigDecimal amount, String idempotencyKey) {
        requirePositiveAmount(amount);
        Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return replayTopup(existing, cardId, amount);
        }

        Card card = cardRepository.findByIdForTopup(cardId).orElseThrow(CardNotFoundException::new);
        ensureCardActive(card);

        Transaction topupTransaction = reserveIdempotencyKey(card, TYPE_TOPUP, amount, idempotencyKey);
        card.credit(amount);
        cardRepository.save(card);

        topupTransaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(topupTransaction);

        log.info("Topup success cardId={} amount={} idempotencyKey={}", cardId, amount, idempotencyKey);
        return card;
    }

    @Transactional(noRollbackFor = InsufficientBalanceException.class)
    public Card spend(UUID cardId, BigDecimal amount, String idempotencyKey) {
        requirePositiveAmount(amount);
        Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return replaySpend(existing, cardId, amount);
        }

        Card card = cardRepository.findByIdForSpend(cardId).orElseThrow(CardNotFoundException::new);
        ensureCardActive(card);

        Transaction spendTransaction = reserveIdempotencyKey(card, TYPE_SPEND, amount, idempotencyKey);

        try {
            card.debit(amount);
        } catch (IllegalStateException ex) {
            if (!"Insufficient balance".equals(ex.getMessage())) {
                throw ex;
            }
            spendTransaction.setStatus(TransactionStatus.DECLINED);
            transactionRepository.save(spendTransaction);
            log.warn("Spend declined cardId={} amount={} idempotencyKey={} reason=INSUFFICIENT_BALANCE",
                    cardId, amount, idempotencyKey);
            throw new InsufficientBalanceException();
        }
        cardRepository.save(card);

        spendTransaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(spendTransaction);

        log.info("Spend success cardId={} amount={} idempotencyKey={}", cardId, amount, idempotencyKey);
        return card;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(UUID cardId) {
        cardRepository.findById(cardId).orElseThrow(CardNotFoundException::new);
        return transactionRepository.findByCard_Id(cardId)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    private void ensureCardActive(Card card) {
        if (card.getStatus() != CardStatus.ACTIVE)
            throw new CardNotActiveException();
    }

    private Transaction reserveIdempotencyKey(Card card, String type, BigDecimal amount, String idempotencyKey) {
        try {
            Transaction pending = new Transaction(card, type, amount, TransactionStatus.PENDING, idempotencyKey);
            return transactionRepository.saveAndFlush(pending);
        } catch (DataIntegrityViolationException ex) {
            Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
            UUID expectedCardId = TYPE_ISSUANCE.equals(type) ? null : card.getId();
            validateIdempotentReplay(existing, expectedCardId, type, amount);
            //throw new IdempotencyInProgressException();
            return waitForCompletion(idempotencyKey);
        }
    }

    private Card replayCreate(Transaction existing, String expectedName, BigDecimal expectedAmount) {
        validateIdempotentReplay(existing, null, TYPE_ISSUANCE, expectedAmount);
        if (existing.getStatus() == TransactionStatus.PENDING) {
            //throw new IdempotencyInProgressException();
            existing = waitForCompletion(existing.getIdempotencyKey());
        }
        log.info("Idempotent replay for issuance idempotencyKey={}", existing.getIdempotencyKey());
        Card existingCard = getCard(existing.getCard().getId());
        if (!Objects.equals(existingCard.getCardholderName(), expectedName)) {
            throw new IdempotencyConflictException("Idempotency key reused with different request payload");
        }
        return existingCard;
    }

    private Card replayTopup(Transaction existing, UUID cardId, BigDecimal amount) {
        validateIdempotentReplay(existing, cardId, TYPE_TOPUP, amount);
        if (existing.getStatus() == TransactionStatus.PENDING) {
            //throw new IdempotencyInProgressException();
            existing = waitForCompletion(existing.getIdempotencyKey());
        }
        log.info("Idempotent replay for topup cardId={} idempotencyKey={}", cardId, existing.getIdempotencyKey());
        return getCard(cardId);
    }

    private Card replaySpend(Transaction existing, UUID cardId, BigDecimal amount) {
        validateIdempotentReplay(existing, cardId, TYPE_SPEND, amount);
        if (existing.getStatus() == TransactionStatus.PENDING) {
            //throw new IdempotencyInProgressException();
            existing = waitForCompletion(existing.getIdempotencyKey());
        }
        log.info("Idempotent replay for spend cardId={} idempotencyKey={}", cardId, existing.getIdempotencyKey());
        if (existing.getStatus() == TransactionStatus.DECLINED) {
            throw new InsufficientBalanceException();
        }
        return getCard(cardId);
    }

    private void validateIdempotentReplay(Transaction existing, UUID expectedCardId, String expectedType, BigDecimal expectedAmount) {
        if (!expectedType.equals(existing.getType())
                || (expectedCardId != null && !Objects.equals(expectedCardId, existing.getCard().getId()))
                || (expectedAmount != null && existing.getAmount().compareTo(expectedAmount) != 0)) {
            throw new IdempotencyConflictException("Idempotency key reused with different request payload");
        }
    }

    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }

    private void requireNonNegativeAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    private Transaction waitForCompletion(String key) {

        int maxAttempts = 50;          // 50 * 100ms = 5 seconds max wait
        int attempt = 0;

        while (attempt < maxAttempts) {

            Transaction tx = transactionRepository
                    .findByIdempotencyKey(key)
                    .orElseThrow();

            if (tx.getStatus() != TransactionStatus.PENDING) {
                return tx;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for idempotent result");
            }

            attempt++;
        }

        throw new RuntimeException("Timeout waiting for idempotent request completion");
    }


}
