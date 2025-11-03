package com.ecommerce.merchant_service.dto;

import lombok.Data;

@Data
public class MerchantResponse {
    private Long merchantId;
    private Long userId;
    private String name;
    private String email;
    private String role;
}
