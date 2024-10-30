package com.example.orderservice.top.domain;

import com.example.orderservice.domain.OrderEntity;
import jakarta.persistence.*;
import lombok.Builder;
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

    private String orderId;
    private String userId;
    private String productId;
    private Integer qty;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    public Outbox() {
    }

    @Builder
    public Outbox(Aggregate aggregate, OutboxStatus status, String orderId, String userId, String productId,
                  Integer qty, LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        this.aggregate = aggregate;
        this.status = status;
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.qty = qty;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }

    public void changeSuccess(Outbox outbox) {
        outbox.status = OutboxStatus.SEND_SUCCESS;
    }

    public void changeFail(Outbox outbox) {
        outbox.status = OutboxStatus.SEND_FAIL;
    }
}
