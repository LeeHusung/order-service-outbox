package com.example.orderservice.top.v3;

import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    @Async
    @TransactionalEventListener
    public void handler(OrderExternalEventMessagePayload payload) {
        log.info("orderDto: {}", payload.toString());
        log.info("OrderEventMessageListener Task executed");

        Outbox outbox = outboxRepository.findByOrderId(payload.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Outbox not found for order: " + payload.getOrderId()));
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload))
                    .handleAsync((result, ex) -> {
                        if (ex != null) {
                            log.info("Kafka 전송 실패: ", ex);
                            outbox.changeFail(outbox);
                            outboxRepository.save(outbox);
                        } else {
                            outbox.changeSuccess(outbox);
                            outboxRepository.save(outbox);
                        }
                        return null;
                    });
        } catch (Exception e) {
            log.info("처리 중 오류 발생: ", e);
            outbox.changeFail(outbox);
            System.out.println("hit");
            log.info("outbox.status ={}", outbox.getStatus());
            outboxRepository.save(outbox);
        }
    }
}
