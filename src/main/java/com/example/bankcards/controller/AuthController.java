package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register/user")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        return registerUserWithRole(request, Set.of(Role.ROLE_USER));
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")  // Только админ может создавать других админов
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        return registerUserWithRole(request, Set.of(Role.ROLE_ADMIN, Role.ROLE_USER));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow();

        return ResponseEntity.ok(new JwtResponse(jwt, user.getUsername(), user.getEmail()));
    }

    private ResponseEntity<?> registerUserWithRole(RegisterRequest request, Set<Role> roles) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email is already in use!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRoles(roles);

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully with roles: " + roles);
    }

    @GetMapping("/debug/check-password")
    public String checkPassword(@RequestParam String raw, @RequestParam String encoded) {
        boolean matches = passwordEncoder.matches(raw, encoded);
        return "Raw: " + raw + ", Encoded: " + encoded + ", Matches: " + matches;
    }

    @GetMapping("/debug/generate-hash")
    public String generateHash(@RequestParam String password) {
        return passwordEncoder.encode(password);
    }
}
