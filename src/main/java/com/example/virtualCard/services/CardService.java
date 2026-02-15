package com.example.virtualCard.services;

import com.example.virtualCard.entity.Card;
import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.enums.TransactionStatus;
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
        return cardRepository.findById(id).orElse(null);
    }
    public Card topup(UUID cardId, BigDecimal amount) {
        Card card = cardRepository.findById(cardId).orElse(null);

        card.setBalance(card.getBalance().add(amount));
        cardRepository.save(card);

        transactionRepository.save(new Transaction(cardId, "TOPUP", amount, TransactionStatus.SUCCESS));

        return card;
    }

    public Card spend(UUID cardId, BigDecimal amount) {
        Card card = cardRepository.findById(cardId).orElse(null);

        if (card.getBalance().compareTo(amount) < 0) {
            transactionRepository.save(new Transaction(cardId, "SPEND", amount, TransactionStatus.DECLINED));
            return card;
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
