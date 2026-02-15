package com.example.virtualCard.controller;

import com.example.virtualCard.dto.CreateCardRequest;
import com.example.virtualCard.entity.Card;
import com.example.virtualCard.services.CardService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {
    private final CardService cardService;
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }
    @PostMapping("/create")
    public Card create(@RequestBody CreateCardRequest req)
    {
        return cardService.createCard(req.getCardholderName(),req.getInitialBalance());
    }
    @GetMapping("/{id}")
    public Card get(@PathVariable UUID id)
    {
        return cardService.getCard(id);
    }
}
