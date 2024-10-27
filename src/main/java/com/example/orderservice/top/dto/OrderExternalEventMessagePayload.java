package com.example.orderservice.top.dto;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.top.domain.Outbox;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderExternalEventMessagePayload {

    private String orderId;
    private String userId;
    private String productId;
    private Integer qty;

    public static OrderExternalEventMessagePayload from(OrderDto orderDto) {
        return OrderExternalEventMessagePayload.builder()
                .orderId(orderDto.getOrderId())
                .userId(orderDto.getUserId())
                .productId(orderDto.getProductId())
                .qty(orderDto.getQty())
                .build();
    }

    public static OrderExternalEventMessagePayload outboxToPayload(Outbox outbox) {
        return OrderExternalEventMessagePayload
                .builder()
                .orderId(outbox.getOrderId())
                .qty(outbox.getQty())
                .productId(outbox.getProductId())
                .build();
    }
}
