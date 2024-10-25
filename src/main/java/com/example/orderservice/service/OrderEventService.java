package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.dto.OrderExternalEventMessagePayload;
import com.example.orderservice.event.OrderEventMessageListener;
import com.example.orderservice.jpa.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventService implements OrderService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderEventMessageListener orderEventService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    //예외 처리 여기서 하는게 이게 맞냐? 애초에 하는게 맞냐?
    public OrderDto createOrder(OrderDto orderDto) {
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        /**
         * outbox pattern with polling with EventListener
         */
        orderRepository.save(orderEntity);

        //event 수신 메서드가 가 두 개 이상이면?
        applicationEventPublisher.publishEvent(OrderExternalEventMessagePayload.from(orderDto));

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