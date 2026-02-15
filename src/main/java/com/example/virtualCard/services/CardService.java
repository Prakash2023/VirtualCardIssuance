package com.example.virtualCard.services;

import com.example.virtualCard.entity.Card;
import com.example.virtualCard.repository.CardRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CardService {
    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
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


}
