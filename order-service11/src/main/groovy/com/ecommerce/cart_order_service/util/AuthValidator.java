package com.ecommerce.cart_order_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public Map<String, Object> validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        try {
            String token = authHeader.substring(7);

           
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = parsed.getBody();

            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", claims.get("userId"));
            userData.put("email", claims.getSubject()); 
            userData.put("role", claims.get("role"));

            System.out.println("Token validated: " + userData);

            return userData;

        } catch (ExpiredJwtException e) {
            System.out.println("Token expired: " + e.getMessage());
            throw new RuntimeException("Unauthorized: Token expired");
        } catch (SignatureException e) {
            System.out.println("Invalid signature: " + e.getMessage());
            throw new RuntimeException("Unauthorized: Invalid signature");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unauthorized: Invalid or expired token");
        }
    }
}
