package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.model.User;
import com.example.user_service.service.AuthService;
import com.example.user_service.service.RefreshTokenService;
import com.example.user_service.service.UserAlreadyExistsException;
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
    private AuthService authService;

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
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) throws UserAlreadyExistsException {
        AuthResponse response = userService.registerUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.loginUser(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest body) {
        AuthResponse response = refreshTokenService.refreshAccessToken(body.getRefreshToken());
        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest body) {
        refreshTokenService.revoke(body.getRefreshToken());
        return ResponseEntity.ok().build();
    }
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> claims = authService.validateToken(authHeader);
        return ResponseEntity.ok(claims);
    }


}











