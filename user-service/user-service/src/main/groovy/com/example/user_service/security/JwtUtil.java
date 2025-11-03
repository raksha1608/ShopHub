package com.example.user_service.security;

import com.example.user_service.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessExpMillis;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-minutes:60}") long accessExpMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpMillis = accessExpMinutes * 60 * 1000;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claims(Map.of(
                        "role", user.getRole().name(),
                        "userId", user.getId(),
                        "fullName",user.getName()

                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpMillis)))
                .signWith(secretKey, Jwts.SIG.HS384)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
    }

    public String getEmail(String token) {
        return parse(token).getBody().getSubject();
    }

    public String getRole(String token) {
        Object r = parse(token).getBody().get("role");
        return r == null ? null : r.toString();
    }

    public boolean isValid(String token) {
        try { parse(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
