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
//    @Transactional
    //예외 처리 여기서 하는게 이게 맞냐? 애초에 하는게 맞냐?
    public OrderDto createOrder(OrderDto orderDto) {
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        Outbox outbox = mapToOutbox(orderEntity);

        //이 코드는 왜 이 코드가 끝날 때 commit or rollback 이 발생하는게 아닌가?
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            orderRepository.save(orderEntity);
            outboxRepository.save(outbox);
        });

        //consumer에서 멱등하게 설계했기에, send에 성공하고 delete에 실패해도 retry가 재전송보내면 중복 처리 안됨.
        //send에 실패하고 delete 성공하면? send는 비동기인가?
        //send에서 무한 로딩 걸림. 밑에 delete도 안됨. 즉, 동기식으로 작동함.
        //근데 요청이 계속 전송이 돼.
        //kafka를 중간에 끊으면, 서버에서 정보를 기억하고 있다가 계속 재시도를 보냄. 이후 로직도 진행됨.
        //kafka를 끊고 서버를 시작하면, 서버는 정보를 모르기에 무한로딩 걸린다. 요청이 안됨. 이후 로직 진행 안됨.
        //하지만 나는 서버가 재시도를 통해 catalog가 처리하는 것 보다,
        //outbox 테이블에 저장된 내용을 retry로 읽어서 kafka가 다시 살아났을 때 데이터 일관성 맞추고 싶어
        //지금은 kafka가 성공하든 실패하든 delete는 무조건 발생한다. 이러면 안돼. callback이든, 동기든 send가 실패하면 delete도 실패하고
        //send가 성공해야 delete도 성공해야 한다.
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
