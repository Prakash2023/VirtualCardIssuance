package com.example.virtualCard.services;

import com.example.virtualCard.dto.TransactionResponse;
import com.example.virtualCard.entity.Card;
import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.enums.CardStatus;
import com.example.virtualCard.enums.TransactionStatus;
import com.example.virtualCard.exception.CardNotActiveException;
import com.example.virtualCard.exception.CardNotFoundException;
import com.example.virtualCard.exception.IdempotencyInProgressException;
import com.example.virtualCard.exception.InsufficientBalanceException;
import com.example.virtualCard.repository.CardRepository;
import com.example.virtualCard.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
    private final IdempotencyService idempotencyService;

    public CardService(
            CardRepository cardRepository,
            TransactionRepository transactionRepository,
            IdempotencyService idempotencyService
    ) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public Card createCard(String name, BigDecimal amount, String idempotencyKey) {
        requireNonNegativeAmount(amount, "initialBalance");
        Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return idempotencyService.replayCreate(existing, name, amount);
        }

        Card card = cardRepository.save(new Card(name, amount));
        Transaction issuance;
        try {
            issuance = idempotencyService.reserveIdempotencyKey(card, TYPE_ISSUANCE, amount, idempotencyKey);
        } catch (IdempotencyInProgressException ex) {
            cardRepository.delete(card);
            throw ex;
        }

        if (!Objects.equals(issuance.getCard().getId(), card.getId())) {
            cardRepository.delete(card);
            return idempotencyService.replayCreate(issuance, name, amount);
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
            return idempotencyService.replayTopup(existing, cardId, amount);
        }

        Card card = cardRepository.findByIdForTopup(cardId).orElseThrow(CardNotFoundException::new);
        ensureCardActive(card);

        Transaction topupTransaction = idempotencyService.reserveIdempotencyKey(card, TYPE_TOPUP, amount, idempotencyKey);
        if (topupTransaction.getStatus() != TransactionStatus.PENDING) {
            return idempotencyService.replayTopup(topupTransaction, cardId, amount);
        }

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
            return idempotencyService.replaySpend(existing, cardId, amount);
        }

        Card card = cardRepository.findByIdForSpend(cardId).orElseThrow(CardNotFoundException::new);
        ensureCardActive(card);

        Transaction spendTransaction = idempotencyService.reserveIdempotencyKey(card, TYPE_SPEND, amount, idempotencyKey);
        if (spendTransaction.getStatus() != TransactionStatus.PENDING) {
            return idempotencyService.replaySpend(spendTransaction, cardId, amount);
        }

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
}
