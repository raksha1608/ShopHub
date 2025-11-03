package com.ecommerce.cart_order_service.Service;

import com.ecommerce.cart_order_service.Model.CartItems;
import com.ecommerce.cart_order_service.Repository.CartItemsRepository;
import com.ecommerce.cart_order_service.util.AuthValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartItemsServiceTest {

    @Mock
    private CartItemsRepository repo;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuthValidator authValidator;

    @InjectMocks
    private CartItemsService cartService;

    private String authHeader;
    private Map<String, Object> claims;
    private CartItems sampleItem;
    private Object sampleProductResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        authHeader = "Bearer mockToken";
        claims = Map.of("userId", 1L, "role", "END_USER");

        sampleItem = CartItems.builder()
                .id(1L)
                .userId(1L)
                .productId("p123")
                .merchantId(1L)
                .quantity(2)
                .price(100.0)
                .build();

        sampleProductResponse = Map.of(
                "merchants", List.of(Map.of(
                        "merchant_id", "1",
                        "stock", "10"
                ))
        );
    }

    @Test
    void addToCart_shouldReturnSuccess_whenValidInput() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(claims);
        when(restTemplate.getForObject(anyString(), eq(Object.class)))
                .thenReturn(sampleProductResponse);
        when(repo.findByUserIdAndProductIdAndMerchantId(anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(repo.save(any(CartItems.class))).thenReturn(sampleItem);

        // Act
        ResponseEntity<?> response = cartService.addToCart(authHeader, sampleItem);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue((Boolean) body.get("success"));
        verify(repo, times(1)).save(any(CartItems.class));
    }

    @Test
    void addToCart_shouldReturnForbidden_whenRoleIsNotEndUser() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(Map.of("role", "MERCHANT"));

        // Act
        ResponseEntity<?> response = cartService.addToCart(authHeader, sampleItem);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(repo, never()).save(any());
    }

    @Test
    void addToCart_shouldReturnBadRequest_whenProductNotFound() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(claims);
        when(restTemplate.getForObject(anyString(), eq(Object.class)))
                .thenThrow(new RuntimeException("Product not found"));

        // Act
        ResponseEntity<?> response = cartService.addToCart(authHeader, sampleItem);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(repo, never()).save(any());
    }

    @Test
    void addToCart_shouldReturnError_whenStockInsufficient() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(claims);
        Object lowStock = Map.of("merchants", List.of(Map.of("merchant_id", "1", "stock", "1")));
        when(restTemplate.getForObject(anyString(), eq(Object.class))).thenReturn(lowStock);
        when(repo.findByUserIdAndProductIdAndMerchantId(anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = cartService.addToCart(authHeader, sampleItem);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(repo, never()).save(any());
    }

    @Test
    void getCart_shouldReturnUserCart() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(claims);
        when(repo.findByUserId(1L)).thenReturn(List.of(sampleItem));

        // Act
        List<CartItems> result = cartService.getCart(authHeader, 1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("p123", result.get(0).getProductId());
        verify(repo).findByUserId(1L);
    }

    @Test
    void updateCart_shouldUpdateExistingItem() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(claims);
        when(repo.findByUserIdAndProductIdAndMerchantId(1L, "p123", 1L))
                .thenReturn(Optional.of(sampleItem));
        when(repo.save(any(CartItems.class))).thenReturn(sampleItem);

        // Act
        ResponseEntity<?> response = cartService.updateCart(authHeader, sampleItem);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(repo).save(any(CartItems.class));
    }

    @Test
    void updateCart_shouldReturnNotFound_whenItemMissing() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(claims);
        when(repo.findByUserIdAndProductIdAndMerchantId(1L, "p123", 1L))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = cartService.updateCart(authHeader, sampleItem);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(repo, never()).save(any());
    }

    @Test
    void removeFromCart_shouldDeleteItem() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(claims);

        // Act
        ResponseEntity<?> response = cartService.removeFromCart(authHeader, sampleItem);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(repo).deleteByUserIdAndProductIdAndMerchantId(1L, "p123", 1L);
    }

    @Test
    void clearCart_shouldDeleteAllUserItems() {
        // Arrange
        when(authValidator.validateToken(authHeader)).thenReturn(claims);

        // Act
        ResponseEntity<?> response = cartService.clearCart(authHeader, 1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(repo).deleteByUserId(1L);
    }
}
