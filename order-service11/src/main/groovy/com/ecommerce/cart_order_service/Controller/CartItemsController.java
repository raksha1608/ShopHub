package com.ecommerce.cart_order_service.Controller;

import com.ecommerce.cart_order_service.Model.CartItems;
import com.ecommerce.cart_order_service.Service.CartItemsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@CrossOrigin(origins = "*")
public class CartItemsController {

    @Autowired
    private CartItemsService cartService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CartItems item) {
        return cartService.addToCart(authHeader, item);
    }

    @GetMapping("/get/{userId}")
    public ResponseEntity<List<CartItems>> getCart(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(cartService.getCart(authHeader, userId));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateCart(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CartItems item) {
        return cartService.updateCart(authHeader, item);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFromCart(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CartItems item) {
        return cartService.removeFromCart(authHeader, item);
    }

    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<?> clearCart(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("userId") Long userId) {
        return cartService.clearCart(authHeader, userId);
    }
}
