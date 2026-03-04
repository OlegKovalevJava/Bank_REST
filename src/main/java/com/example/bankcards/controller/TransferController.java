package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/transfers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Переводы", description = "Операции перевода средств между своими картами")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Перевод между своими картами",
            description = "Выполняет перевод средств с одной карты пользователя на другую")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Перевод успешно выполнен"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или недостаточно средств"),
            @ApiResponse(responseCode = "403", description = "Карта заблокирована или недостаточно прав"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("POST /api/user/transfers - transfer request received: from {} to {}, amount: {}",
                request.getSourceCardId(), request.getDestinationCardId(), request.getAmount());

        TransferResponse response = transferService.transfer(request);

        log.info("Transfer completed successfully. Transaction ID: {}", response.getTransactionId());

        return ResponseEntity.ok(response);
    }
}
