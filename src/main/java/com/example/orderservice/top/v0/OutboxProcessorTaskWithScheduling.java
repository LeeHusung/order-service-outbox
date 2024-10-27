package com.example.orderservice.jpa;

import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class OutboxProcessorTaskWithScheduling {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper;

    public OutboxProcessorTaskWithScheduling(OutboxRepository outboxRepository,
                                             KafkaTemplate<String, String> kafkaTemplate, @Value("${kafka.topic}") String topic, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.objectMapper = objectMapper;
    }

//    @Scheduled(fixedRate = 10000)
    @Transactional
    public void process() throws JsonProcessingException {
        log.info("Task executed");
        List<Outbox> outboxes = outboxRepository.findTop10ByIsDelivered(false);

        //kafka가 다운되었을때 true 처리를 하면?
        for (Outbox outbox : outboxes) {
            kafkaTemplate.send(topic,  objectMapper.writeValueAsString(OrderExternalEventMessagePayload.outboxToPayload(outbox)));

            //이거 save() 안해도 true로 변경되나?
            outbox.setIsDelivered(true);
        }
    }

}
