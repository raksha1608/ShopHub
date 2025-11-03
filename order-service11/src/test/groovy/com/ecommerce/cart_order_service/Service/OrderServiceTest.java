package com.ecommerce.cart_order_service.Service;

import com.ecommerce.cart_order_service.Model.CartItems;
import com.ecommerce.cart_order_service.Model.Order;
import com.ecommerce.cart_order_service.Model.OrderItems;
import com.ecommerce.cart_order_service.Repository.CartItemsRepository;
import com.ecommerce.cart_order_service.Repository.OrderRepository;
import com.ecommerce.cart_order_service.util.AuthValidator;
import com.ecommerce.cart_order_service.util.NotificationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OrderServiceTest {

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private CartItemsRepository cartRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AuthValidator authValidator;

    @Autowired
    private NotificationUtil notificationUtil;

    public ResponseEntity<?> placeOrder(String authHeader, Order order) {
        try {
            Map<String, Object> claims = authValidator.validateToken(authHeader);
            String role = (String) claims.get("role");
            Long userId = Long.valueOf(claims.get("userId").toString());

            if (!"END_USER".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only END_USER can place orders"));
            }

            // Verify product stock for each item
            for (OrderItems item : order.getItems()) {
                String productUrl = "http://product-service/api/products/" + item.getProductId();
                Object response = restTemplate.getForObject(productUrl, Object.class);

                if (!(response instanceof Map<?, ?> responseMap)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid product response"));
                }

                List<Map<String, Object>> merchants = (List<Map<String, Object>>) responseMap.get("merchants");
                Optional<Map<String, Object>> merchantData = merchants.stream()
                        .filter(m -> m.get("merchant_id").equals(item.getMerchantId()))
                        .findFirst();

                if (merchantData.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Merchant not found for product " + item.getProductId()));
                }

                int stock = Integer.parseInt(merchantData.get().get("stock").toString());
                if (item.getQuantity() > stock) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Insufficient stock for product " + item.getProductId()));
                }
            }

            // Save order
            order.setUserId(userId);
            Order savedOrder = orderRepo.save(order);

            // Clear cart
            cartRepo.deleteByUserId(userId);

            // Update stock
            for (OrderItems item : order.getItems()) {
                try {
                    String updateUrl = "http://product-service/api/products/" + item.getProductId()
                            + "/update-stock?merchantId=" + item.getMerchantId()
                            + "&quantity=" + item.getQuantity();
                    restTemplate.postForEntity(updateUrl, null, Void.class);
                } catch (Exception e) {
                    System.err.println("Failed to update stock for product " + item.getProductId() + ": " + e.getMessage());
                }
            }

            // Send async notification
            try {
                notificationUtil.sendOrderConfirmation(savedOrder, "user@example.com"); // replace with actual user email if available
            } catch (Exception e) {
                System.err.println("Failed to send order confirmation: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", savedOrder.getId()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    public List<Order> getOrders(String authHeader, Long userId) {
        Map<String, Object> claims = authValidator.validateToken(authHeader);
        String role = (String) claims.get("role");
        Long tokenUserId = Long.valueOf(claims.get("userId").toString());

        if (!"END_USER".equals(role) || !tokenUserId.equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return orderRepo.findByUserId(userId);
    }
}
