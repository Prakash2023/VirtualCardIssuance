package com.example.virtualCard.controller;

import com.example.virtualCard.dto.AmountRequest;
import com.example.virtualCard.dto.CreateCardRequest;
import com.example.virtualCard.entity.Card;
import com.example.virtualCard.entity.Transaction;
import com.example.virtualCard.services.CardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    @PostMapping("/{id}/topup")
    public Card topup(@PathVariable UUID id, @RequestBody AmountRequest req) {
        return cardService.topup(id, req.getAmount());
    }
    @PostMapping("/{id}/spend")
    public Card spend(@PathVariable UUID id, @RequestBody AmountRequest req) {
        return cardService.spend(id, req.getAmount());
    }

    @GetMapping("/{id}/transactions")
    public List<Transaction> transactions(@PathVariable UUID id) {
        return cardService.getTransactions(id);
    }
}
