package com.example.user_service.controller;

import com.example.user_service.dto.UserResponse;
import com.example.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {
    @Autowired
    private  UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/user/me")
    public Object me(Authentication auth) {
        if (auth == null || auth.getName() == null) return Map.of("error", "Unauthenticated");
        var user = userService.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @GetMapping("/user/profile")
    public Map<String, Object> getProfile(Authentication auth) {
        if (auth == null) return Map.of("error", "Missing or invalid Authorization header");
        return Map.of(
                "email", auth.getName(),
                "message", "Secure profile data fetched successfully!"
        );
    }
}
