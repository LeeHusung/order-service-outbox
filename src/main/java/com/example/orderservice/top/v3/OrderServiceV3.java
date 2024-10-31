package com.example.orderservice.top.v3;

import com.example.orderservice.domain.OrderEntity;
import com.example.orderservice.domain.OrderRepository;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV3 implements OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        /**
         * outbox pattern with polling with EventListener - 4번
         */
        orderEntity.setCreatedAt(LocalDateTime.now());
        orderRepository.save(orderEntity);
        log.info("orderEntity: {}", orderEntity.toString());
        applicationEventPublisher.publishEvent(OrderExternalEventMessagePayload.from(orderEntity));

        log.info("Kafka Producer sent data from the Order microservice: " + orderDto);
        OrderDto returnValue = mapper.map(orderEntity, OrderDto.class);
        return returnValue;
    }

    @Override
    public OrderDto getOrderByOrderId(String orderId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);
        OrderDto orderDto = new ModelMapper().map(orderEntity, OrderDto.class);

        return orderDto;
    }

    @Override
    public Iterable<OrderEntity> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
