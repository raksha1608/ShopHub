package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.model.User;
import com.example.user_service.service.RefreshTokenService;
import com.example.user_service.service.UserService;
import com.example.user_service.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private  JwtUtil jwtUtil;
    @Autowired
    private  RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(userService.encodePassword(request.getPassword()));
        user.setRole(User.Role.valueOf(request.getRole())); // expects END_USER or MERCHANT
        userService.saveUser(user);

        String access = jwtUtil.generateAccessToken(user);
        String refresh = refreshTokenService.create(user.getEmail()).getToken();
        return ResponseEntity.ok(new AuthResponse(access, refresh));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return userService.findByEmail(request.getEmail())
                .map(user -> {
                    if (userService.checkPassword(request.getPassword(), user.getPassword())) {
                        String access = jwtUtil.generateAccessToken(user);
                        String refresh = refreshTokenService.create(user.getEmail()).getToken();
                        return ResponseEntity.ok(new AuthResponse(access, refresh));
                    } else {
                        return ResponseEntity.status(401).body("Invalid credentials");
                    }
                })
                .orElse(ResponseEntity.status(404).body("User not found"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest body) {
        var token = refreshTokenService.validate(body.getRefreshToken());
        var user = userService.findByEmail(token.getUserEmail())
                .orElseThrow(() -> new RuntimeException("User no longer exists"));
        String access = jwtUtil.generateAccessToken(user);
        return ResponseEntity.ok(new AuthResponse(access, token.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest body) {
        refreshTokenService.revoke(body.getRefreshToken());
        return ResponseEntity.ok().build();
    }
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            var claims = jwtUtil.parse(token).getBody();
            return ResponseEntity.ok(Map.of(
                    "email", claims.getSubject(),
                    "role", claims.get("role"),
                    "userId", claims.get("userId")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }
    }

}
