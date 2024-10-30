package com.example.orderservice.top.v1;

import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.messagequeue.KafkaProducer;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
//@Component
public class OutboxRetryTask {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "10000")
    public void retry() {
        List<Outbox> outboxEntities = outboxRepository.findAllBefore(LocalDateTime.now().minusSeconds(60));
        for (Outbox outbox : outboxEntities) {
            log.info("try retry outbox Id = {}", outbox.getId());
            OrderExternalEventMessagePayload payload = OrderExternalEventMessagePayload.outboxToPayload(outbox);
            try {
                kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload)).thenAcceptAsync(
                        x -> outboxRepository.delete(outbox)
                ).exceptionallyAsync(e -> {
                    log.error("Kafka 전송 실패: ", e);
                    outbox.changeFail(outbox);
                    return null;
                });
            } catch (JsonProcessingException e) {
                log.error("Error serializing payload for outbox id: {}", outbox.getId(), e);
            }
        }
    }
}
