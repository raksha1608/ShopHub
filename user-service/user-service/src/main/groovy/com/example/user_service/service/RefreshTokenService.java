package com.example.user_service.service;

import com.example.user_service.dto.AuthResponse;
import com.example.user_service.model.RefreshToken;
import com.example.user_service.model.User;
import com.example.user_service.repository.RefreshTokenRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class RefreshTokenService {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository repo;

    private final SecureRandom random = new SecureRandom();

    @Value("${jwt.refresh-exp-days:15}")
    private int refreshExpDays;

    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    public RefreshToken create(String userEmail) {
        RefreshToken rt = new RefreshToken();
        rt.setUserEmail(userEmail);
        rt.setToken(generateSecureToken());
        rt.setExpiresAt(Instant.now().plus(refreshExpDays, ChronoUnit.DAYS));
        return repo.save(rt);
    }

    public RefreshToken validate(String token) {
        return repo.findByToken(token)
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token"));
    }
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        RefreshToken token = validate(refreshToken);
        User user = userRepository.findByEmail(token.getUserEmail())
                .orElseThrow(() -> new RuntimeException("User no longer exists"));
        String access = jwtUtil.generateAccessToken(user);
        return new AuthResponse(access, token.getToken());
    }

    public void revoke(String token) {
        repo.findByToken(token).ifPresent(t -> { t.setRevoked(true); repo.save(t); });
    }

    public void revokeAllForUser(String email) { repo.deleteByUserEmail(email); }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}