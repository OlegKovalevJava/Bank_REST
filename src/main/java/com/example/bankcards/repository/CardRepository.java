package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    Page<Card> findByUserId(UUID userId, Pageable pageable);

    Optional<Card> findByIdAndUserId(UUID cardId, UUID userId);

    boolean existsByIdAndUserId(UUID cardId, UUID userId);

    Optional<Card> findByCardNumber(String cardNumber);

    @Query("SELECT c FROM Card c JOIN FETCH c.user")
    Page<Card> findAllWithUsers(Pageable pageable);
}
