package com.example.bankcards.controller;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Админ: Карты", description = "Управление всеми картами (только для ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "Получить все карты всех пользователей")
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("GET /api/admin/cards - admin fetching all cards");
        Page<CardResponse> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить карты конкретного пользователя")
    public ResponseEntity<Page<CardResponse>> getCardsByUserId(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("GET /api/admin/cards/user/{} - admin fetching user cards", userId);
        Page<CardResponse> cards = cardService.getCardsByUserId(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    @PatchMapping("/{cardId}/activate")
    @Operation(summary = "Активировать карту")
    public ResponseEntity<CardResponse> activateCard(@PathVariable UUID cardId) {
        log.info("PATCH /api/admin/cards/{}/activate - admin activating card", cardId);
        CardResponse activatedCard = cardService.activateCard(cardId);
        return ResponseEntity.ok(activatedCard);
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Удалить карту")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID cardId) {
        log.info("DELETE /api/admin/cards/{} - admin deleting card", cardId);
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
