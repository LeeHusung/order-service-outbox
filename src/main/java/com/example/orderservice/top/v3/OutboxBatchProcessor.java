package com.example.orderservice.top.v3;

import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.domain.OutboxStatus;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
                                @Value("${kafka.outbox.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Scheduled(fixedDelay = 60000) // 매 1분마다 실행
    public void processFailedMessages() {
        List<Outbox> failedOutboxEntries = outboxRepository.findByStatuses(Arrays.asList(OutboxStatus.INIT, OutboxStatus.SEND_FAIL));

        log.info("Processing {} failed outbox messages", failedOutboxEntries.size());

        //비동기로 안했을 때 왜 위에 1개가 있는데, 아래 로그가 하나도 안나오지?
        //status도  init이다
        for (Outbox outbox : failedOutboxEntries) {
            OrderExternalEventMessagePayload payload = OrderExternalEventMessagePayload.outboxToPayload(outbox);
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
                log.info("Error serializing payload for outbox id: {}", outbox.getId(), e);
            }
        }
    }

}
