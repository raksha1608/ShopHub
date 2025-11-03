package com.ecommerce.cart_order_service.util;

import com.ecommerce.cart_order_service.Model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class NotificationUtil {

    @Autowired
    private RestTemplate restTemplate;


    @Value("${email.service.url}")
    private String emailServiceUrl;


    @Async
    public void sendOrderConfirmation(Order order, String userEmail) {
        System.out.println("Calling Email Service for order: " + order.getId());


        Map<String, Object> requestBody = Map.of(
                "orderId", order.getId(),
                "userId", order.getUserId(),
                "userEmail", userEmail,
                "totalAmount", order.getTotalAmount(),
                "items", order.getItems()
        );

        try {

            restTemplate.postForEntity(
                    emailServiceUrl + "/email/send-order-confirmation",
                    requestBody,
                    String.class
            );
            System.out.println("Successfully notified Email Service for order: " + order.getId());

        } catch (Exception e) {

            System.err.println(" Failed to call Email Service: " + e.getMessage());
        }
    }
}