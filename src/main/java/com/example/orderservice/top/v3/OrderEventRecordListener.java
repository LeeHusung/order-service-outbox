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

    //커밋되기 전이라서 = 즉 tx가 끝나기 전이라서 jpa쓰면 영속성 컨텍스트에 남아있음. @CreateAt은 insert 쿼리가 실행될 때 값이 들어가기 때문에
    //이 아래 메서드를 탈 때는 createAt 값이 존재하지 않고 당연히 payload에 값이 없고 outbox에도 값이 안들어간다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordHandler(OrderExternalEventMessagePayload payload) {
        log.info("outbox save");
        log.info("OrderEventRecordListener Task executed");

        outboxRepository.save(mapToOutbox(payload));
    }

    //일단 여기서 만들고 위치 고민해봐야 함.
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
