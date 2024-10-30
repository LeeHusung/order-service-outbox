package com.example.orderservice.top.v2;

import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.domain.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

//@Component
@Slf4j
public class OutboxBatchProcessor {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OutboxBatchProcessor(KafkaTemplate<String, String> kafkaTemplate,
                                OutboxRepository outboxRepository,
                                ObjectMapper objectMapper,
                                @Value("${kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Scheduled(fixedDelay = 60000) // 매 1분마다 실행
    public void processFailedMessages() {
        List<Outbox> failedOutboxEntries = outboxRepository.findByStatuses(Arrays.asList(OutboxStatus.INIT, OutboxStatus.SEND_FAIL));

        log.info("Processing {} failed outbox messages", failedOutboxEntries.size());

        for (Outbox outbox : failedOutboxEntries) {
            OrderExternalEventMessagePayload payload = OrderExternalEventMessagePayload.outboxToPayload(outbox);
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
            } catch (JsonProcessingException e) {
                log.error("Error serializing payload for outbox id: {}", outbox.getId(), e);
            }
        }
    }

}
