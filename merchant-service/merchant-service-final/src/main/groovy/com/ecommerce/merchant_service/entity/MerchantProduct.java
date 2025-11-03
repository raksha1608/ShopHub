package com.ecommerce.merchant_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "merchant_products")
public class MerchantProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long merchantId;
    private String productId;
    private BigDecimal price;
    private Integer stock;
}
