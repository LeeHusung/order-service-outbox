package com.example.orderservice.top.v0;

import com.example.orderservice.top.domain.Aggregate;
import com.example.orderservice.domain.OrderEntity;
import com.example.orderservice.domain.OrderRepository;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.domain.OutboxStatus;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

//@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV0 implements OrderService {
    private final OutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        Outbox outbox = mapToOutbox(orderEntity);

        orderRepository.save(orderEntity);
        outboxRepository.save(outbox);

        return mapper.map(orderEntity, OrderDto.class);
    }

    //일단 여기서 만들고 위치 고민해봐야 함.
    private Outbox mapToOutbox(OrderEntity orderEntity) {
        return  Outbox.builder().
                aggregate(Aggregate.ORDER)
                .status(OutboxStatus.INIT)
                .createdAt(orderEntity.getCreatedAt())
                .qty(orderEntity.getQty())
                .productId(orderEntity.getProductId())
                .userId(orderEntity.getUserId())
                .orderId(orderEntity.getOrderId())
                .build();
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
