package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user/cards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Карты", description = "Управление банковскими картами пользователя")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "Получить все карты текущего пользователя",
            description = "Возвращает список карт с пагинацией")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список карт получен"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "Параметры пагинации") Pageable pageable) {

        log.info("GET /api/user/cards - fetching cards for current user");
        Page<CardResponse> cards = cardService.getCurrentUserCards(pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Получить карту по ID",
            description = "Возвращает детальную информацию о конкретной карте")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта найдена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<CardResponse> getCardById(
            @PathVariable @Parameter(description = "UUID карты") UUID cardId) {

        log.info("GET /api/user/cards/{} - fetching card details", cardId);
        CardResponse card = cardService.getCardById(cardId);
        return ResponseEntity.ok(card);
    }

    @PostMapping
    @Operation(summary = "Создать новую карту",
            description = "Создаёт карту для текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Карта успешно создана"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody @Parameter(description = "Данные для создания карты") CardRequest request) {

        log.info("POST /api/user/cards - creating new card");
        CardResponse createdCard = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @PatchMapping("/{cardId}/block")
    @Operation(summary = "Заблокировать карту",
            description = "Блокирует карту (доступно владельцу или ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта заблокирована"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<CardResponse> blockCard(
            @PathVariable @Parameter(description = "UUID карты") UUID cardId) {

        log.info("PATCH /api/user/cards/{}/block - blocking card", cardId);
        CardResponse blockedCard = cardService.blockCard(cardId);
        return ResponseEntity.ok(blockedCard);
    }
}
