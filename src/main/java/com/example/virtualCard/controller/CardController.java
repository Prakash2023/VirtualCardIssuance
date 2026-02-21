package com.example.virtualCard.controller;

import com.example.virtualCard.dto.AmountRequest;
import com.example.virtualCard.dto.CreateCardRequest;
import com.example.virtualCard.dto.TransactionResponse;
import com.example.virtualCard.entity.Card;
import jakarta.validation.Valid;
import com.example.virtualCard.services.CardService;
import com.example.virtualCard.services.TransactionQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {
    private final CardService cardService;
    private final TransactionQueryService transactionQueryService;

    public CardController(CardService cardService, TransactionQueryService transactionQueryService) {
        this.cardService = cardService;
        this.transactionQueryService = transactionQueryService;
    }
    @PostMapping("/create")
    public Card create(@Valid @RequestBody CreateCardRequest req)
    {
        return cardService.createCard(req.getCardholderName(), req.getInitialBalance(), req.getIdempotencyKey());
    }
    @GetMapping("/{id}")
    public Card get(@PathVariable UUID id)
    {
        return cardService.getCard(id);
    }
    @PostMapping("/{id}/topup")
    public Card topup(@PathVariable UUID id, @Valid @RequestBody AmountRequest req) {
        return cardService.topup(id, req.getAmount(), req.getIdempotencyKey());
    }
    @PostMapping("/{id}/spend")
    public Card spend(@PathVariable UUID id, @Valid @RequestBody AmountRequest req) {
        return cardService.spend(id, req.getAmount(), req.getIdempotencyKey());
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionResponse> transactions(@PathVariable UUID id) {
        return transactionQueryService.getTransactions(id);
    }
}
