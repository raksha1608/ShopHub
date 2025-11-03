package com.ecommerce.cart_order_service.Service;

import com.ecommerce.cart_order_service.Model.CartItems;
import com.ecommerce.cart_order_service.Repository.CartItemsRepository;
import com.ecommerce.cart_order_service.util.AuthValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartItemsService {

    @Autowired
    private CartItemsRepository repo;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AuthValidator authValidator;

    @Value("${product.service.url}")
    private String productServiceUrl;

    public ResponseEntity<?> addToCart(String authHeader, CartItems item) {
        Map<String, Object> claims = authValidator.validateToken(authHeader);
        String role = (String) claims.get("role");

        if (!"END_USER".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only END_USER can add to cart."));
        }

        // Validate product existence
        Object productData;
        try {
            productData = restTemplate.getForObject(
                    productServiceUrl + "/products/" + item.getProductId(),
                    Object.class
            );
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid Product ID: " + item.getProductId()));
        }

        if (item.getMerchantId() == null || item.getMerchantId() <= 0) {
            item.setMerchantId(1L);
        }

        int availableStock = getAvailableStock(productData, item.getMerchantId());
        Optional<CartItems> existing = repo.findByUserIdAndProductIdAndMerchantId(
                item.getUserId(), item.getProductId(), item.getMerchantId()
        );

        int currentQty = existing.map(CartItems::getQuantity).orElse(0);
        int newQty = currentQty + item.getQuantity();

        if (newQty > availableStock) {
            return ResponseEntity.status(400).body(Map.of("error",
                    "Not enough stock! Available: " + availableStock));
        }

        CartItems saved;
        if (existing.isPresent()) {
            CartItems old = existing.get();
            old.setQuantity(newQty);
            old.setPrice(item.getPrice());
            saved = repo.save(old);
        } else {
            saved = repo.save(item);
        }

        return ResponseEntity.ok(Map.of("success", true, "item", saved));
    }


    private int getAvailableStock(Object productData, Long merchantId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) productData;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> merchants =
                    (List<Map<String, Object>>) product.get("merchants");

            if (merchants != null) {
                for (Map<String, Object> m : merchants) {
                    long mid = Long.parseLong(m.get("merchant_id").toString());
                    if (mid == merchantId) {
                        return Integer.parseInt(m.get("stock").toString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing stock: " + e.getMessage());
        }
        return 0;
    }

    public List<CartItems> getCart(String authHeader, Long userId) {
        authValidator.validateToken(authHeader);
        return repo.findByUserId(userId);
    }

    public ResponseEntity<?> updateCart(String authHeader, CartItems item) {
        authValidator.validateToken(authHeader);
        Optional<CartItems> existing = repo.findByUserIdAndProductIdAndMerchantId(
                item.getUserId(), item.getProductId(), item.getMerchantId());

        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Item not found"));
        }

        CartItems existingItem = existing.get();
        existingItem.setQuantity(item.getQuantity());
        existingItem.setPrice(item.getPrice());
        repo.save(existingItem);

        return ResponseEntity.ok(Map.of("success", true, "message", "Cart updated"));
    }

    @Transactional
    public ResponseEntity<?> removeFromCart(String authHeader, CartItems item) {
        authValidator.validateToken(authHeader);
        repo.deleteByUserIdAndProductIdAndMerchantId(
                item.getUserId(), item.getProductId(), item.getMerchantId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Item removed"));
    }

    @Transactional
    public ResponseEntity<?> clearCart(String authHeader, Long userId) {
        authValidator.validateToken(authHeader);
        repo.deleteByUserId(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Cart cleared"));
    }
}
