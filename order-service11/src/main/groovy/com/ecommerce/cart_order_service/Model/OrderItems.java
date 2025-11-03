package com.ecommerce.cart_order_service.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product ID cannot be blank")
    @Column(name = "product_id", nullable = false)
    private String productId;

    @NotNull(message = "Merchant ID cannot be blank")
    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.01", inclusive = true, message = "Price must be greater than 0")
    @Column(nullable = false)
    private Double price;
}
