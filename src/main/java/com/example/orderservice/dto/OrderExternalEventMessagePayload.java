package com.example.orderservice.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
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

}
