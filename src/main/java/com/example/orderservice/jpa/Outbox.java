package com.example.orderservice.jpa;

import com.example.orderservice.dto.OrderExternalEventMessagePayload;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate")
    private Aggregate aggregate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OutboxStatus status;

    @Column(name = "message", length = 500)
    private String message;

    private String orderId;
    private String productId;
    private Integer qty;

    @Column(name = "is_delivered", nullable = false)
    private Boolean isDelivered = false;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    public Outbox() {
    }

    public Outbox(Aggregate aggregate, OutboxStatus status, String orderId, String message, Boolean isDelivered) {
        this.aggregate = aggregate;
        this.status = status;
        this.orderId = orderId;
        this.message = message;
        this.isDelivered = isDelivered;
    }

    public void changeSuccess(Outbox outbox) {
        outbox.status = OutboxStatus.SEND_SUCCESS;
    }

    public void changeFail(Outbox outbox) {
        outbox.status = OutboxStatus.SEND_FAIL;
    }
}