package com.example.orderservice.top.v3;

import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.example.orderservice.top.domain.Aggregate;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.example.orderservice.top.domain.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

//@Component
@Slf4j
public class OrderEventRecordListener {

    private final OutboxRepository outboxRepository;

    public OrderEventRecordListener(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordHandler(OrderExternalEventMessagePayload payload) {
        log.info("outbox save");
        log.info("OrderEventRecordListener Task executed");

        outboxRepository.save(mapToOutbox(payload));
    }

    private Outbox mapToOutbox(OrderExternalEventMessagePayload payload) {
        return  Outbox.builder().
                aggregate(Aggregate.ORDER)
                .status(OutboxStatus.INIT)
                .createdAt(payload.getCreatedAt())
                .qty(payload.getQty())
                .productId(payload.getProductId())
                .userId(payload.getUserId())
                .orderId(payload.getOrderId())
                .build();
    }

}
