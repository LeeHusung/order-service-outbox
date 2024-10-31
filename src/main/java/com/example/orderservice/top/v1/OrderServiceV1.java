package com.example.orderservice.top.v1;

import com.example.orderservice.top.domain.Aggregate;
import com.example.orderservice.domain.OrderEntity;
import com.example.orderservice.domain.OrderRepository;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.domain.OutboxStatus;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.messagequeue.KafkaProducer;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

//@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV1 implements OrderService {
    private final OutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    @Value("${kafka.outbox.topic}")
    private final String topic;


    @Override
    public OrderDto createOrder(OrderDto orderDto) {
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        Outbox outbox = mapToOutbox(orderEntity);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            orderRepository.save(orderEntity);
            outboxRepository.save(outbox);
        });

        OrderExternalEventMessagePayload payload = OrderExternalEventMessagePayload.outboxToPayload(outbox);
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload)).thenAccept(
                    (x) -> outboxRepository.delete(outbox)
            );
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }

        log.info("Kafka Producer sent data from the Order microservice: " + orderDto);

        return mapper.map(orderEntity, OrderDto.class);
    }

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
