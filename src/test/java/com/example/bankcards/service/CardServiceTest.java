package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

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
    private CardService cardService;

    private User testUser;
    private User adminUser;
    private Card testCard;
    private CardRequest cardRequest;
    private CardResponse cardResponse;
    private UUID userId;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cardId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(Role.ROLE_USER));

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setPassword("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER));

        testCard = new Card();
        testCard.setId(cardId);
        testCard.setCardNumber("1234567890123456");
        testCard.setCardHolderName("Test User");
        testCard.setExpiryDate(LocalDate.now().plusYears(1));
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setBalance(new BigDecimal("1000.00"));
        testCard.setUser(testUser);

        cardRequest = new CardRequest();
        cardRequest.setCardNumber("9876543210987654");
        cardRequest.setCardHolderName("Test User");
        cardRequest.setExpiryDate(LocalDate.now().plusYears(2));
        cardRequest.setBalance(new BigDecimal("500.00"));

        cardResponse = new CardResponse();
        cardResponse.setId(cardId);
        cardResponse.setMaskedCardNumber("**** **** **** 3456");
        cardResponse.setCardHolderName("Test User");
        cardResponse.setExpiryDate(testCard.getExpiryDate());
        cardResponse.setStatus(CardStatus.ACTIVE);
        cardResponse.setBalance(new BigDecimal("1000.00"));

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        lenient().when(userDetails.getUsername()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Успешное получение карт текущего пользователя")
    void getCurrentUserCards_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByUserId(userId, pageable)).thenReturn(cardPage);
        when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);
        // Act
        Page<CardResponse> result = cardService.getCurrentUserCards(pageable);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(cardId);
        verify(cardRepository).findByUserId(userId, pageable);
    }

    @Test
    @DisplayName("Успешное получение карты по ID")
    void getCardById_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(testCard));
        when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);
        // Act
        CardResponse result = cardService.getCardById(cardId);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
    }

    @Test
    @DisplayName("Ошибка: карта не найдена при получении по ID")
    void getCardById_NotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> cardService.getCardById(cardId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card not found or access denied");
    }

    @Test
    @DisplayName("Успешное создание карты")
    void createCard_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardMapper.toEntity(cardRequest)).thenReturn(testCard);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);
        // Act
        CardResponse result = cardService.createCard(cardRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMaskedCardNumber()).isEqualTo("**** **** **** 3456");
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("Успешная блокировка карты владельцем")
    void blockCard_ByOwner_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);
        // Act
        CardResponse result = cardService.blockCard(cardId);
        // Assert
        assertThat(result).isNotNull();
        assertThat(testCard.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository).save(testCard);
    }

    @Test
    @DisplayName("Ошибка блокировки чужой карты (не админ)")
    void blockCard_Unauthorized() {
        // Arrange
        Card otherCard = new Card();
        otherCard.setId(UUID.randomUUID());
        otherCard.setUser(new User()); // другой пользователь

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(otherCard.getId())).thenReturn(Optional.of(otherCard));

        // Act & Assert
        assertThatThrownBy(() -> cardService.blockCard(otherCard.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You don't have permission");
    }

    @Test
    @DisplayName("Успешное получение всех карт админом")
    void getAllCards_Admin_Success() {
        // Arrange
        setupAdminContext();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(cardRepository.findAll(pageable)).thenReturn(cardPage);
        when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);

        // Act
        Page<CardResponse> result = cardService.getAllCards(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Успешное получение карт пользователя админом")
    void getCardsByUserId_Admin_Success() {
        // Arrange
        setupAdminContext();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(cardRepository.findByUserId(userId, pageable)).thenReturn(cardPage);
        when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);
        // Act
        Page<CardResponse> result = cardService.getCardsByUserId(userId, pageable);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).findByUserId(userId, pageable);
    }

    @Test
    @DisplayName("Успешная активация карты админом")
    void activateCard_Admin_Success() {
        // Arrange
        setupAdminContext();
        testCard.setStatus(CardStatus.BLOCKED);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);
        // Act
        CardResponse result = cardService.activateCard(cardId);
        // Assert
        assertThat(result).isNotNull();
        assertThat(testCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository).save(testCard);
    }

    @Test
    @DisplayName("Успешное удаление карты админом")
    void deleteCard_Admin_Success() {
        // Arrange
        setupAdminContext();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(cardRepository.existsById(cardId)).thenReturn(true);
        doNothing().when(cardRepository).deleteById(cardId);
        // Act
        cardService.deleteCard(cardId);
        // Assert
        verify(cardRepository).deleteById(cardId);
    }

    @Test
    @DisplayName("Ошибка: обычный пользователь пытается получить все карты")
    void getAllCards_User_Unauthorized() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // Act & Assert
        assertThatThrownBy(() -> cardService.getAllCards(PageRequest.of(0, 20)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Admin access required");
    }

    @Test
    @DisplayName("Ошибка: пользователь не найден")
    void getCurrentUserCards_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> cardService.getCurrentUserCards(PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    private void setupAdminContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("admin");
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                Set.of(Role.ROLE_ADMIN).stream()
                        .map(role -> (org.springframework.security.core.GrantedAuthority) role::name)
                        .toList()
        );
    }
}
