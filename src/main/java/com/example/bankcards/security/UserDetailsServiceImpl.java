package com.example.bankcards.security;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j  // 👈 Добавь эту аннотацию, если её нет
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("🔍 Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("❌ User not found: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        log.info("✅ User found: {}", user.getUsername());
        log.info("🔑 Password hash from DB: {}", user.getPassword());

        // Создаем UserDetails для Spring Security
        org.springframework.security.core.userdetails.User securityUser =
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

        log.info("🎯 UserDetails created for: {}", securityUser.getUsername());
        log.info("🔒 Password hash in UserDetails: {}", securityUser.getPassword());

        return securityUser;
    }
}
