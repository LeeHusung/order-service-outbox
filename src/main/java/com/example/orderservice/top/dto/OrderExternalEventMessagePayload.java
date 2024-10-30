package com.example.orderservice.top.dto;

import com.example.orderservice.domain.OrderEntity;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.top.domain.Outbox;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderExternalEventMessagePayload {

    private String orderId;
    private String userId;
    private String productId;
    private Integer qty;
    private LocalDateTime createdAt;

    public static OrderExternalEventMessagePayload from(OrderEntity orderEntity) {
        return OrderExternalEventMessagePayload.builder()
                .orderId(orderEntity.getOrderId())
                .userId(orderEntity.getUserId())
                .productId(orderEntity.getProductId())
                .createdAt(orderEntity.getCreatedAt())
                .qty(orderEntity.getQty())
                .build();
    }

    public static OrderExternalEventMessagePayload outboxToPayload(Outbox outbox) {
        return OrderExternalEventMessagePayload.builder()
                .orderId(outbox.getOrderId())
                .userId(outbox.getUserId())
                .qty(outbox.getQty())
                .productId(outbox.getProductId())
                .createdAt(outbox.getCreatedAt())
                .build();
    }
}
