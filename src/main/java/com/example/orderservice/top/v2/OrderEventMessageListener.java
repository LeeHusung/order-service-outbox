package com.example.orderservice.top.v2;

import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

//@Component
@Slf4j
public class OrderEventMessageListener {

    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    public OrderEventMessageListener(@Value("${kafka.outbox.topic}") String topic, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, OutboxRepository outboxRepository) {
        this.topic = topic;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }

    //이 비동기 메서드를 실행하기 전에 서버가 죽으면 init으로 남음. = 정상
    //이 비동기 메서드를 실행하다가 kafka가 에러 발생함. send_fail로 변경됨 = 정상
    //실행하다가 kafka로 메시지 발행 전에 서버가 죽음. init으로 남음 = 정상
    //실행하다가 kafka로 메시지 발행 성공 = send_success로 변경됨 = 정상
    //결국 이 비동기 메서드를 모두 실행하기 전에 서버가 죽으면 init으로 남는 상황을 방지하기 위해
    // 서버가 죽을 때 기존 실행중인 비동기 메서드를 모두 실행 완료할 때까지 설정함으로써 init을 없앨 수 있음.
    //but 양날의 검이다. 급하게 죽어야 하는데, 이 설정으로 인해 계속 살아있을 수 있음.

    //init으로 남아있으면 이 listener 문제일 가능성이 큼. kafka로 send() 도 못한거니, 이 메서드가 실행이 안되었을 것이다.
    //send_fail이라면 kafka로 보내지긴 했는데, 실패했단 것임.
    @Async
    @TransactionalEventListener
    public void handler(OrderExternalEventMessagePayload payload) {
        log.info("orderDto: {}", payload.toString());
        log.info("OrderEventMessageListener Task executed");

        Outbox outbox = outboxRepository.findByOrderId(payload.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Outbox not found for order: " + payload.getOrderId()));

        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload)).thenAcceptAsync(
                    x -> {
                        outbox.changeSuccess(outbox);
                        outboxRepository.save(outbox);
                    }
            ).exceptionallyAsync(e -> {
                log.error("Kafka 전송 실패: ", e);
                outbox.changeFail(outbox);
                return null;
            });
        } catch (Exception e) {
            log.error("처리 중 오류 발생: ", e);
            outbox.changeFail(outbox);
        }
    }
}
