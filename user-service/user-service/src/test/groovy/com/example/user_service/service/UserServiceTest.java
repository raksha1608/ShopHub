package com.example.user_service.service;

import com.example.user_service.dto.AuthRequest;
import com.example.user_service.dto.AuthResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.model.RefreshToken;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private RefreshToken sampleRefreshToken;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(userService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(userService, "refreshTokenService", refreshTokenService);

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("Alice");
        sampleUser.setEmail("alice@example.com");
        sampleUser.setPassword("hashedpwd");
        sampleUser.setRole(User.Role.END_USER);

        sampleRefreshToken = new RefreshToken();
        sampleRefreshToken.setToken("refresh-token");
        sampleRefreshToken.setUser(sampleUser);
    }

    @Test
    void registerUser_shouldCreateNewUser_whenEmailNotExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtil.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.create(anyString())).thenReturn(sampleRefreshToken);

        AuthRequest request = new AuthRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setRole("END_USER");

        AuthResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        AuthRequest request = new AuthRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setRole("END_USER");

        assertThrows(RuntimeException.class, () -> userService.registerUser(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginUser_shouldReturnTokens_whenCredentialsAreValid() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashedpwd")).thenReturn(true);
        when(jwtUtil.generateAccessToken(sampleUser)).thenReturn("access-token");
        when(refreshTokenService.create("alice@example.com")).thenReturn(sampleRefreshToken);

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        AuthResponse response = userService.loginUser(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void loginUser_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        assertThrows(RuntimeException.class, () -> userService.loginUser(request));
    }

    @Test
    void loginUser_shouldThrow_whenPasswordMismatch() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashedpwd")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        assertThrows(RuntimeException.class, () -> userService.loginUser(request));
    }

    @Test
    void checkPassword_shouldDelegateToEncoder() {
        when(passwordEncoder.matches("a", "b")).thenReturn(true);
        assertTrue(userService.checkPassword("a", "b"));
        verify(passwordEncoder).matches("a", "b");
    }

    @Test
    void saveUser_shouldCallRepository() {
        userService.saveUser(sampleUser);
        verify(userRepository).save(sampleUser);
    }

    @Test
    void findByEmail_shouldReturnUserOptional() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));

        Optional<User> result = userService.findByEmail("alice@example.com");

        assertTrue(result.isPresent());
        assertEquals(sampleUser, result.get());
    }
}
