package com.example.orderservice.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class OrderDto implements Serializable {
    private Long id;
    private String productId;
    private Integer qty;
    private Integer unitPrice;
    private Integer totalPrice;

    private String orderId;
    private String userId;

    private LocalDateTime createdAt = LocalDateTime.now();
}
