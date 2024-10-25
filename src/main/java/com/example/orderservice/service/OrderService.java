package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.jpa.OrderEntity;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDetails) throws JsonProcessingException;
    OrderDto getOrderByOrderId(String orderId);
    Iterable<OrderEntity> getOrdersByUserId(String userId);
}
