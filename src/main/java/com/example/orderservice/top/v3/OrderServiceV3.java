package com.example.orderservice.top.v3;

import com.example.orderservice.top.domain.Aggregate;
import com.example.orderservice.domain.OrderEntity;
import com.example.orderservice.domain.OrderRepository;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.domain.OutboxStatus;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV3 implements OrderService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    //예외 처리 여기서 하는게 이게 맞냐? 애초에 하는게 맞냐?
    //v3에서는 예외처리 사라지나? OrderEventRecordListener 얘가 예외 처리 진행함. 이것도 장점인가?
    public OrderDto createOrder(OrderDto orderDto) throws JsonProcessingException {
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        /**
         * outbox pattern with polling with EventListener - 4번
         */
//        //event 수신 메서드가 가 두 개 이상이면?
        applicationEventPublisher.publishEvent(OrderExternalEventMessagePayload.from(orderDto));

        log.info("Kafka Producer sent data from the Order microservice: " + orderDto);
        OrderDto returnValue = mapper.map(orderEntity, OrderDto.class);
        return returnValue;
    }

    private void recordEventToOutboxTable(OrderDto orderDto) throws JsonProcessingException {
        Outbox outbox = new Outbox(Aggregate.ORDER, OutboxStatus.INIT, orderDto.getOrderId(), objectMapper.writeValueAsString(orderDto), false);
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
