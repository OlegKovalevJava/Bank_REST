package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.SameCardTransferException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private TransferService transferService;

    private User testUser;
    private Card sourceCard;
    private Card destCard;
    private TransferRequest validRequest;
    private UUID sourceCardId;
    private UUID destCardId;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        sourceCardId = UUID.randomUUID();
        destCardId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(Role.ROLE_USER));

        sourceCard = new Card();
        sourceCard.setId(sourceCardId);
        sourceCard.setCardNumber("1234567890123456");
        sourceCard.setBalance(new BigDecimal("1000.00"));
        sourceCard.setStatus(CardStatus.ACTIVE);
        sourceCard.setUser(testUser);

        destCard = new Card();
        destCard.setId(destCardId);
        destCard.setCardNumber("9876543210987654");
        destCard.setBalance(new BigDecimal("500.00"));
        destCard.setStatus(CardStatus.ACTIVE);
        destCard.setUser(testUser);

        validRequest = new TransferRequest();
        validRequest.setSourceCardId(sourceCardId);
        validRequest.setDestinationCardId(destCardId);
        validRequest.setAmount(new BigDecimal("200.00"));

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        lenient().when(userDetails.getUsername()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Успешный перевод между своими картами")
    void transfer_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findById(destCardId)).thenReturn(Optional.of(destCard));
        when(cardMapper.maskCardNumber(anyString())).thenReturn("**** **** **** 3456", "**** **** **** 7654");
        // Act
        TransferResponse response = transferService.transfer(validRequest);
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        verify(cardRepository, times(2)).save(any(Card.class));
        assertThat(sourceCard.getBalance()).isEqualTo(new BigDecimal("800.00"));
        assertThat(destCard.getBalance()).isEqualTo(new BigDecimal("700.00"));
    }

    @Test
    @DisplayName("Ошибка: исходная карта не найдена")
    void transfer_SourceCardNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Source card not found");
    }

    @Test
    @DisplayName("Ошибка: целевая карта не найдена")
    void transfer_DestCardNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findById(destCardId)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Destination card not found");
    }

    @Test
    @DisplayName("Ошибка: карта принадлежит другому пользователю")
    void transfer_Unauthorized() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        sourceCard.setUser(otherUser);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findById(destCardId)).thenReturn(Optional.of(destCard));
        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(validRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only transfer between your own cards");
    }

    @Test
    @DisplayName("Ошибка: перевод на ту же карту")
    void transfer_SameCard() {
        // Arrange
        validRequest.setDestinationCardId(sourceCardId);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(validRequest))
                .isInstanceOf(SameCardTransferException.class)
                .hasMessageContaining("Cannot transfer to the same card");
    }

    @Test
    @DisplayName("Ошибка: исходная карта заблокирована")
    void transfer_SourceCardBlocked() {
        // Arrange
        sourceCard.setStatus(CardStatus.BLOCKED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findById(destCardId)).thenReturn(Optional.of(destCard));
        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(validRequest))
                .isInstanceOf(CardBlockedException.class)
                .hasMessageContaining("Source card is not active");
    }

    @Test
    @DisplayName("Ошибка: целевая карта заблокирована")
    void transfer_DestCardBlocked() {
        // Arrange
        destCard.setStatus(CardStatus.BLOCKED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findById(destCardId)).thenReturn(Optional.of(destCard));
        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(validRequest))
                .isInstanceOf(CardBlockedException.class)
                .hasMessageContaining("Destination card is not active");
    }

    @Test
    @DisplayName("Ошибка: недостаточно средств")
    void transfer_InsufficientFunds() {
        // Arrange
        validRequest.setAmount(new BigDecimal("1500.00"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findById(destCardId)).thenReturn(Optional.of(destCard));
        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(validRequest))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("Ошибка: пользователь не найден")
    void transfer_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Проверка маскировки номера карты в ответе")
    void transfer_MaskCardNumber() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
        when(cardRepository.findById(destCardId)).thenReturn(Optional.of(destCard));
        when(cardMapper.maskCardNumber("1234567890123456")).thenReturn("**** **** **** 3456");
        when(cardMapper.maskCardNumber("9876543210987654")).thenReturn("**** **** **** 7654");
        // Act
        TransferResponse response = transferService.transfer(validRequest);
        // Assert
        assertThat(response.getSourceCardMask()).isEqualTo("**** **** **** 3456");
        assertThat(response.getDestinationCardMask()).isEqualTo("**** **** **** 7654");
    }
}
