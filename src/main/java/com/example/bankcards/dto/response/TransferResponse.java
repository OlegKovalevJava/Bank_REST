package com.example.bankcards.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransferResponse {
    private UUID transactionId;
    private String sourceCardMask;
    private String destinationCardMask;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String status;
    private String message;
}
