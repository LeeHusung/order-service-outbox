package com.example.orderservice.top.v2;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.top.domain.Aggregate;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.domain.OutboxStatus;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.example.orderservice.domain.*;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

//@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV2 implements OrderService {
    private final OutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
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
         * outbox pattern with polling with EventListener - 3번
         */
        orderRepository.save(orderEntity);
        saveOutbox(orderEntity);
        //이거 이렇게하면 메시지에 발행 시간 저장 안될걸?? 테스트해봐야함.

        //event 수신 메서드가 가 두 개 이상이면?
        applicationEventPublisher.publishEvent(OrderExternalEventMessagePayload.from(orderEntity));

        log.info("Kafka Producer sent data from the Order microservice: " + orderEntity);
        OrderDto returnValue = mapper.map(orderEntity, OrderDto.class);
        return returnValue;
    }

    private void saveOutbox(OrderEntity orderEntity) {
        Outbox outbox = Outbox.builder().
                aggregate(Aggregate.ORDER)
                .status(OutboxStatus.INIT)
                .createdAt(orderEntity.getCreatedAt())
                .qty(orderEntity.getQty())
                .productId(orderEntity.getProductId())
                .userId(orderEntity.getUserId())
                .orderId(orderEntity.getOrderId())
                .build();
        outboxRepository.save(outbox);
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
