package com.ecommerce.cart_order_service.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id", "merchant_id"})
)
public class CartItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID cannot be null")
    @Positive(message = "User ID must be a positive number")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Product ID cannot be blank")
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Min(value = 1, message = "Merchant ID must be positive")
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(nullable = false)
    private double price;
}
