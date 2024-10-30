package com.example.orderservice.top.v0;

import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.domain.OutboxStatus;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

//@Component
@Slf4j
public class OutboxProcessorTaskWithScheduling {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper;

    public OutboxProcessorTaskWithScheduling(OutboxRepository outboxRepository,
                                             KafkaTemplate<String, String> kafkaTemplate,
                                             @Value("${kafka.outbox.topic}") String topic, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void process() {
        log.info("Task executed");
        List<Outbox> outboxes = outboxRepository.findByStatuses(Arrays.asList(OutboxStatus.INIT, OutboxStatus.SEND_FAIL));

        for (Outbox outbox : outboxes) {
            log.info("try retry outbox Id = {}", outbox.getId());
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
