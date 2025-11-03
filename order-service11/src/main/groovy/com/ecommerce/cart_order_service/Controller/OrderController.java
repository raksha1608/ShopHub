package com.ecommerce.cart_order_service.Controller;

import com.ecommerce.cart_order_service.Model.Order;
import com.ecommerce.cart_order_service.Service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody Order order) {
        return orderService.placeOrder(authHeader, order);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Order>> getOrders(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(orderService.getOrders(authHeader, userId));
    }
}
