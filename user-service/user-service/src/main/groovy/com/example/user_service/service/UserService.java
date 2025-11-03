package com.example.user_service.service;

import com.example.user_service.dto.AuthRequest;
import com.example.user_service.dto.AuthResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.model.RefreshToken;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

//import static sun.security.util.KeyUtil.validate;

@Service
public class UserService {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private  RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
// i have wrutten for cleaner practice...not needed
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void saveUser(User user) { userRepository.save(user); }

    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    @Transactional
    public AuthResponse registerUser(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");

        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.valueOf(request.getRole()));

        userRepository.save(user);

        String access = jwtUtil.generateAccessToken(user);
        String refresh = refreshTokenService.create(user.getEmail()).getToken();

        return new AuthResponse(access, refresh);
    }
    public AuthResponse loginUser(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) {
            throw new RuntimeException("Invalid credentials");
        }

        String access = jwtUtil.generateAccessToken(user);
        String refresh = refreshTokenService.create(user.getEmail()).getToken();
        return new AuthResponse(access, refresh);
    }

    public String encodePassword(String raw) { return passwordEncoder.encode(raw); }
}