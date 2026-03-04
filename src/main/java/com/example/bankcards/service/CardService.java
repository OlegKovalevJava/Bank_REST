package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    // ========== Методы для USER ==========

    public Page<CardResponse> getCurrentUserCards(Pageable pageable) {
        User currentUser = getCurrentUser();
        log.info("Fetching cards for user: {}", currentUser.getUsername());

        return cardRepository.findByUserId(currentUser.getId(), pageable)
                .map(cardMapper::toResponse);
    }

    public CardResponse getCardById(UUID cardId) {
        User currentUser = getCurrentUser();
        log.info("Fetching card {} for user {}", cardId, currentUser.getUsername());

        Card card = cardRepository.findByIdAndUserId(cardId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or access denied"));

        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse createCard(CardRequest request) {
        User currentUser = getCurrentUser();
        log.info("Creating new card for user: {}", currentUser.getUsername());

        Card card = cardMapper.toEntity(request);
        card.setUser(currentUser);

        Card savedCard = cardRepository.save(card);
        log.info("Card created with ID: {}", savedCard.getId());

        return cardMapper.toResponse(savedCard);
    }

    @Transactional
    public CardResponse blockCard(UUID cardId) {
        User currentUser = getCurrentUser();
        log.info("Blocking card {} by user {}", cardId, currentUser.getUsername());

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!card.getUser().getId().equals(currentUser.getId()) && !isCurrentUserAdmin()) {
            throw new UnauthorizedException("You don't have permission to block this card");
        }

        card.setStatus(CardStatus.BLOCKED);
        Card blockedCard = cardRepository.save(card);

        return cardMapper.toResponse(blockedCard);
    }

    // ========== Новые методы для ADMIN ==========

    /**
     * Получение всех карт всех пользователей (только для ADMIN)
     */
    public Page<CardResponse> getAllCards(Pageable pageable) {
        if (!isCurrentUserAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        log.info("Admin fetching all cards");
        return cardRepository.findAll(pageable)
                .map(cardMapper::toResponse);
    }

    /**
     * Получение карт конкретного пользователя (для ADMIN)
     */
    public Page<CardResponse> getCardsByUserId(UUID userId, Pageable pageable) {
        if (!isCurrentUserAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        log.info("Admin fetching cards for user: {}", userId);
        return cardRepository.findByUserId(userId, pageable)
                .map(cardMapper::toResponse);
    }

    /**
     * Активация карты (только для ADMIN)
     */
    @Transactional
    public CardResponse activateCard(UUID cardId) {
        if (!isCurrentUserAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        log.info("Admin activating card: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        card.setStatus(CardStatus.ACTIVE);
        Card activatedCard = cardRepository.save(card);

        return cardMapper.toResponse(activatedCard);
    }

    /**
     * Удаление карты (только для ADMIN)
     */
    @Transactional
    public void deleteCard(UUID cardId) {
        if (!isCurrentUserAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        log.info("Admin deleting card: {}", cardId);

        if (!cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Card not found");
        }

        cardRepository.deleteById(cardId);
        log.info("Card {} deleted successfully", cardId);
    }

    // ========== Вспомогательные методы ==========

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean isCurrentUserAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
