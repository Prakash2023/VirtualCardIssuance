package com.example.virtualCard.repository;

import com.example.virtualCard.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByCard_Id(UUID cardId);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
