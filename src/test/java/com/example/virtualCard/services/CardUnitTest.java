package com.example.virtualCard.services;

import com.example.virtualCard.entity.Card;
import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.enums.CardStatus;
import com.example.virtualCard.enums.TransactionStatus;
import com.example.virtualCard.exception.CardNotActiveException;
import com.example.virtualCard.exception.IdempotencyConflictException;
import com.example.virtualCard.exception.InsufficientBalanceException;
import com.example.virtualCard.repository.CardRepository;
import com.example.virtualCard.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardUnitTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CardService cardService;

    private UUID cardId;
    private Card card;

    @BeforeEach
    void setup() {
        cardId = UUID.randomUUID();
        card = new Card("Test", BigDecimal.valueOf(1000));
        card.setId(cardId);
        card.setStatus(CardStatus.ACTIVE);
    }

    @Test
    void spendSuccess() {
        when(transactionRepository.findByIdempotencyKey("k1"))
                .thenReturn(Optional.empty());

        when(cardRepository.findByIdForSpend(cardId))
                .thenReturn(Optional.of(card));

        when(transactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        cardService.spend(cardId, BigDecimal.valueOf(200), "k1");


        assertThat(BigDecimal.valueOf(800)).isEqualTo(card.getBalance());
    }

    @Test
    void spendInsufficientBalance() {
        when(transactionRepository.findByIdempotencyKey("k2"))
                .thenReturn(Optional.empty());

        when(cardRepository.findByIdForSpend(cardId))
                .thenReturn(Optional.of(card));

        when(transactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(InsufficientBalanceException.class,
                () -> cardService.spend(cardId, BigDecimal.valueOf(1200), "k2"));

        assertThat(BigDecimal.valueOf(1000)).isEqualTo(card.getBalance());

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());

        assertThat(TransactionStatus.DECLINED).isEqualTo(txCaptor.getValue().getStatus());
    }
    @Test
    void spendShouldFailWhenCardInactive() {
        card.setStatus(CardStatus.BLOCKED);

        when(transactionRepository.findByIdempotencyKey("k3"))
                .thenReturn(Optional.empty());

        when(cardRepository.findByIdForSpend(cardId))
                .thenReturn(Optional.of(card));

        assertThrows(CardNotActiveException.class,
                () -> cardService.spend(cardId, BigDecimal.valueOf(10), "k3"));

        verify(transactionRepository, never()).saveAndFlush(any());
    }
    @Test
    void idempotencyReplaySuccessCase() {
        card = cardWithBalance(new BigDecimal("80"));
        Transaction existing = new Transaction(
                card,
                "SPEND",
                BigDecimal.valueOf(20),
                TransactionStatus.SUCCESS,
                "k4"
        );

        when(transactionRepository.findByIdempotencyKey("k4"))
                .thenReturn(Optional.of(existing));

        when(cardRepository.findById(cardId))
                .thenReturn(Optional.of(card));

        Card replay = cardService.spend(cardId, BigDecimal.valueOf(20), "k4");


        assertThat(BigDecimal.valueOf(80)).isEqualTo(replay.getBalance());

        verify(cardRepository, never()).findByIdForSpend(any());
    }

    @Test
    void idempotencyConflict() {
        Transaction existing = new Transaction(
                card,
                "SPEND",
                BigDecimal.valueOf(20),
                TransactionStatus.SUCCESS,
                "k5"
        );

        when(transactionRepository.findByIdempotencyKey("k5"))
                .thenReturn(Optional.of(existing));

        assertThrows(IdempotencyConflictException.class,
                () -> cardService.spend(cardId, BigDecimal.valueOf(25), "k5"));
    }

    @Test
    void topupSuccess() {
        card = cardWithBalance(new BigDecimal("100"));

        when(transactionRepository.findByIdempotencyKey("t1"))
                .thenReturn(Optional.empty());

        when(cardRepository.findByIdForTopup(cardId))
                .thenReturn(Optional.of(card));

        when(transactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Card updated = cardService.topup(cardId, BigDecimal.valueOf(40), "t1");


        assertThat(BigDecimal.valueOf(140)).isEqualTo(updated.getBalance());
        verify(cardRepository).save(card);
    }

    @Test
    void createReplay() {
        card = cardWithBalance(new BigDecimal("200"));
        Transaction existing = new Transaction(
                card,
                "ISSUANCE",
                BigDecimal.valueOf(200),
                TransactionStatus.SUCCESS,
                "c1"
        );

        card.setCardholderName("Alice");

        when(transactionRepository.findByIdempotencyKey("c1"))
                .thenReturn(Optional.of(existing));

        when(cardRepository.findById(cardId))
                .thenReturn(Optional.of(card));

        Card replay = cardService.createCard("Alice", BigDecimal.valueOf(200), "c1");

        assertThat(BigDecimal.valueOf(200)).isEqualTo(replay.getBalance());
        verify(transactionRepository, never()).saveAndFlush(any());
        verify(cardRepository, never()).save(any());
    }

    private Card cardWithBalance(BigDecimal balance) {
        Card value = new Card("Test", balance);
        value.setId(cardId);
        value.setStatus(CardStatus.ACTIVE);
        return value;
    }
}
