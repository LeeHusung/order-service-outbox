package com.example.orderservice.top.v1;

import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.messagequeue.KafkaProducer;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public record OutboxRetryTask(OutboxRepository outboxRepository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper) {

    //    @Scheduled(fixedDelayString = "10000")
    public void retry() {
        List<Outbox> outboxEntities = outboxRepository.findAllBefore(LocalDateTime.now().minusSeconds(60));
        for (Outbox outbox : outboxEntities) {
            log.info("try retry outbox Id = {}", outbox.getId());
            try {
                kafkaTemplate.send("example-catalog-topic", objectMapper.writeValueAsString(
                        OrderExternalEventMessagePayload.outboxToPayload(outbox)));
            } catch (JsonProcessingException ex) {
                log.error("error", ex);
            }

            outboxRepository.delete(outbox);
        }
    }
}
