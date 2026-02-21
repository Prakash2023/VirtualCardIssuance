package com.example.virtualCard.services;

import com.example.virtualCard.dto.TransactionResponse;
import com.example.virtualCard.exception.CardNotFoundException;
import com.example.virtualCard.repository.CardRepository;
import com.example.virtualCard.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TransactionQueryService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public TransactionQueryService(CardRepository cardRepository, TransactionRepository transactionRepository) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(UUID cardId) {
        cardRepository.findById(cardId).orElseThrow(CardNotFoundException::new);
        return transactionRepository.findByCard_Id(cardId)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
