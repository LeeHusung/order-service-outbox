package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.domain.OrderEntity;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDetails);
    OrderDto getOrderByOrderId(String orderId);
    Iterable<OrderEntity> getOrdersByUserId(String userId);
}
