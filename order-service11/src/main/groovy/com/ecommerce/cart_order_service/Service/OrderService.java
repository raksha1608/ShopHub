package com.ecommerce.cart_order_service.Service;

import com.ecommerce.cart_order_service.Model.Order;
import com.ecommerce.cart_order_service.Repository.CartItemsRepository;
import com.ecommerce.cart_order_service.Repository.OrderRepository;
import com.ecommerce.cart_order_service.util.AuthValidator;
import com.ecommerce.cart_order_service.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CartItemsRepository cartRepo;
    private final RestTemplate restTemplate;
    private final AuthValidator authValidator;
    private final NotificationUtil notificationUtil;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Transactional
    public ResponseEntity<?> placeOrder(String authHeader, Order order) {
        // We get the claims map here, which contains the email
        var claims = authValidator.validateToken(authHeader);
        if (!"END_USER".equalsIgnoreCase((String) claims.get("role"))) {
            return ResponseEntity.status(403).body(Map.of("error", "Only END_USER can place orders."));
        }

        // --- Stock Validation Loop ---
        for (var item : order.getItems()) {
            Object productData = restTemplate.getForObject(
                    productServiceUrl + "/products/" + item.getProductId(),
                    Object.class
            );

            int availableStock = getAvailableStock(productData, item.getMerchantId());
            if (item.getQuantity() > availableStock) {
                return ResponseEntity.status(400).body(Map.of(
                        "error", "Insufficient stock for product " + item.getProductId()
                ));
            }
        }

        // --- Save Order and Clear Cart ---
        Order savedOrder = orderRepo.save(order);
        cartRepo.deleteByUserId(order.getUserId());

        // --- Update Stock Loop ---
        for (var item : order.getItems()) {
            try {
                restTemplate.postForEntity(
                        productServiceUrl + "/products/updateStock",
                        Map.of(
                                "productId", item.getProductId(),
                                "merchantId", item.getMerchantId(),
                                "quantity", item.getQuantity()
                        ),
                        Void.class
                );
            } catch (Exception e) {
                System.err.println("⚠ Failed to update stock for " + item.getProductId());
            }
        }

        // --- Send Email Notification ---
        // 1. Get the email from the claims map we fetched at the top
        String userEmail = (String) claims.get("email");

        // 2. Call the notification utility (this is now correct)
        notificationUtil.sendOrderConfirmation(savedOrder, userEmail);


        return ResponseEntity.ok(Map.of(
                "success", true,
                "orderId", savedOrder.getId(),
                "totalAmount", savedOrder.getTotalAmount()
        ));
    }

    private int getAvailableStock(Object productData, String merchantIdStr) {
        try {
            // Using @SuppressWarnings to avoid warnings for unchecked casting
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) productData;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> merchants = (List<Map<String, Object>>) product.get("merchants");

            // Use Long for ID parsing for better safety
            long merchantId = Long.parseLong(merchantIdStr);
            for (Map<String, Object> m : merchants) {
                long mid = Long.parseLong(m.get("merchant_id").toString());
                if (mid == merchantId) {
                    return Integer.parseInt(m.get("stock").toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing product stock: " + e.getMessage());
        }
        return 0;
    }

    public List<Order> getOrders(String authHeader, Long userId) {
        authValidator.validateToken(authHeader);
        return orderRepo.findByUserId(userId);
    }
}