package com.example.user_service.service;

import com.example.user_service.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> validateToken(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            var claims = jwtUtil.parse(token).getBody();

            return Map.of(
                    "email", claims.getSubject(),
                    "role", claims.get("role"),
                    "userId", claims.get("userId")
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token");
        }
    }
}