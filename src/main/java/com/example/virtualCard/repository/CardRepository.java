package com.example.virtualCard.repository;

import com.example.virtualCard.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    @Query("select c from Card c where c.id = :id")
    Optional<Card> findByIdForTopup(@Param("id") UUID id);

    @Query("select c from Card c where c.id = :id")
    Optional<Card> findByIdForSpend(@Param("id") UUID id);
}
