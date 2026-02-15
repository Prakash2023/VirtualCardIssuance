package com.example.virtualCard.services;

import com.example.virtualCard.entity.Card;
import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.enums.CardStatus;
import com.example.virtualCard.enums.TransactionStatus;
import com.example.virtualCard.exception.CardNotActiveException;
import com.example.virtualCard.exception.CardNotFoundException;
import com.example.virtualCard.exception.InsufficientBalanceException;
import com.example.virtualCard.repository.CardRepository;
import com.example.virtualCard.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public CardService(CardRepository cardRepository,TransactionRepository transactionRepository) {
        this.cardRepository = cardRepository;
        this.transactionRepository=transactionRepository;
    }
    public Card createCard(String name, BigDecimal amount)
    {
        Card card=new Card(name,amount);
        return cardRepository.save(card);

    }
    public Card getCard(UUID id)
    {
        return cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException());
    }
    public Card topup(UUID cardId, BigDecimal amount) {
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new CardNotFoundException());
        if (card.getStatus() != CardStatus.ACTIVE)
            throw new CardNotActiveException();

        card.setBalance(card.getBalance().add(amount));
        cardRepository.save(card);

        transactionRepository.save(new Transaction(cardId, "TOPUP", amount, TransactionStatus.SUCCESS));

        return card;
    }

    public Card spend(UUID cardId, BigDecimal amount) {
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new CardNotFoundException());

        if (card.getStatus() != CardStatus.ACTIVE)
            throw new CardNotActiveException();

        if (card.getBalance().compareTo(amount) < 0) {
            transactionRepository.save(new Transaction(cardId, "SPEND", amount, TransactionStatus.DECLINED));
            throw new InsufficientBalanceException();
        }

        card.setBalance(card.getBalance().subtract(amount));
        cardRepository.save(card);

        transactionRepository.save(new Transaction(cardId, "SPEND", amount, TransactionStatus.SUCCESS));

        return card;
    }

    public List<Transaction> getTransactions(UUID cardId) {
        return transactionRepository.findByCardId(cardId);
    }



}
