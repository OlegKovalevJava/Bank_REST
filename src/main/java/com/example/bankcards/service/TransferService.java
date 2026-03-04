package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.SameCardTransferException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransferService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    public TransferResponse transfer(TransferRequest request) {
        User currentUser = getCurrentUser();
        log.info("Transfer requested by user: {} from card: {} to card: {}, amount: {}",
                currentUser.getUsername(), request.getSourceCardId(),
                request.getDestinationCardId(), request.getAmount());

        Card sourceCard = cardRepository.findById(request.getSourceCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found"));

        Card destCard = cardRepository.findById(request.getDestinationCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found"));

        if (!sourceCard.getUser().getId().equals(currentUser.getId()) ||
                !destCard.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only transfer between your own cards");
        }

        if (sourceCard.getId().equals(destCard.getId())) {
            throw new SameCardTransferException("Cannot transfer to the same card");
        }

        if (sourceCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardBlockedException("Source card is not active");
        }

        if (destCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardBlockedException("Destination card is not active");
        }

        if (sourceCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Available: %s, Requested: %s",
                            sourceCard.getBalance(), request.getAmount())
            );
        }

        sourceCard.setBalance(sourceCard.getBalance().subtract(request.getAmount()));
        destCard.setBalance(destCard.getBalance().add(request.getAmount()));

        cardRepository.save(sourceCard);
        cardRepository.save(destCard);

        log.info("Transfer successful! New balances - Source: {}, Dest: {}",
                sourceCard.getBalance(), destCard.getBalance());

        return TransferResponse.builder()
                .transactionId(UUID.randomUUID())
                .sourceCardMask(cardMapper.maskCardNumber(sourceCard.getCardNumber()))
                .destinationCardMask(cardMapper.maskCardNumber(destCard.getCardNumber()))
                .amount(request.getAmount())
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .message("Transfer completed successfully")
                .build();
    }

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
