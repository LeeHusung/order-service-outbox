package com.example.orderservice.event;

import com.example.orderservice.dto.OrderExternalEventMessagePayload;
import com.example.orderservice.domain.Aggregate;
import com.example.orderservice.domain.Outbox;
import com.example.orderservice.domain.OutboxRepository;
import com.example.orderservice.domain.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class OrderEventRecordListener {

    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    public OrderEventRecordListener(ObjectMapper objectMapper, OutboxRepository outboxRepository) {
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }

    //4번
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordHandler(OrderExternalEventMessagePayload payload) throws JsonProcessingException {
        log.info("outbox save");
        log.info("Task executed");

        outboxRepository.save(mapToOutbox(payload));
    }

    //일단 여기서 만들고 위치 고민해봐야 함.
    private Outbox mapToOutbox(OrderExternalEventMessagePayload payload) throws JsonProcessingException {
        return new Outbox(Aggregate.ORDER, OutboxStatus.INIT, payload.getOrderId(), objectMapper.writeValueAsString(payload), false);
    }

}
