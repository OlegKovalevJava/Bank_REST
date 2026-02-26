package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestDataService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;  // 👈 Добавили PasswordEncoder

    @Transactional
    public void createTestData() {
        log.info("Creating test data...");

        // Логируем процесс шифрования
        String rawPassword = "password123";
        log.info("Password before encoding: {}", rawPassword);

        String encodedPassword = passwordEncoder.encode(rawPassword);
        log.info("Password after encoding: {}", encodedPassword);

        // Создаём тестового пользователя с ЗАШИФРОВАННЫМ паролем
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPassword(encodedPassword);

        User savedUser = userRepository.save(testUser);
        log.info("Created user with ID: {}", savedUser.getId());

        // Создаём первую карту
        Card testCard1 = new Card();
        testCard1.setCardNumber("1234567890123456");
        testCard1.setCardHolderName("Test User");
        testCard1.setExpiryDate(LocalDate.now().plusYears(3));
        testCard1.setStatus(CardStatus.ACTIVE);
        testCard1.setBalance(new BigDecimal("1000.00"));
        testCard1.setUser(savedUser);

        Card savedCard1 = cardRepository.save(testCard1);
        log.info("Created card 1: ID={}, number={}", savedCard1.getId(), savedCard1.getCardNumber());

        // Создаём вторую карту
        Card testCard2 = new Card();
        testCard2.setCardNumber("9876543210987654");
        testCard2.setCardHolderName("Test User");
        testCard2.setExpiryDate(LocalDate.now().plusYears(2));
        testCard2.setStatus(CardStatus.ACTIVE);
        testCard2.setBalance(new BigDecimal("500.00"));
        testCard2.setUser(savedUser);

        Card savedCard2 = cardRepository.save(testCard2);
        log.info("Created card 2: ID={}, number={}", savedCard2.getId(), savedCard2.getCardNumber());

        log.info("✅ Test data created successfully!");
        log.info("User: {}", savedUser.getUsername());
        log.info("Total cards: 2");
    }
}
