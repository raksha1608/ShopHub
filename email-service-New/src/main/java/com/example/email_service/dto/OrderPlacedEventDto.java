package com.example.email_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class OrderPlacedEventDto {
    @NotNull
    private Long orderId;

    @Email
    private String userEmail;

    @NotNull
    private Double totalAmount;

    @NotEmpty
    private List<OrderItemsDto> items;
}