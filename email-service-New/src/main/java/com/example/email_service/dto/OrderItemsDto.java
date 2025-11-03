package com.example.email_service.dto;

import lombok.Data;

@Data
public class OrderItemsDto {
    private String productId;
    private Integer quantity;
    private Double price;
}