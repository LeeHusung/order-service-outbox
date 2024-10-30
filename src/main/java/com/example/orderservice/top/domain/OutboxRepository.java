package com.example.orderservice.top.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

//    List<Outbox> findTop10ByIsDelivered(boolean status);

    //outbox 테이블에 현재시간(-60초)보다 먼저 생성된 것이 남아있다면 찾기
    @Query("SELECT o FROM Outbox o WHERE o.createdAt <= :nowTime")
    List<Outbox> findAllBefore(LocalDateTime nowTime);

    Optional<Outbox> findByOrderId(String orderId);

    //인덱스
    //성공 레코드 삭제 (delete 는 지양하는게 좋음) x
    //쿼리 튜닝 필요함. 성능 느림.
    //발행된 순서대로 정렬해서 가져오므로써 메시지 발행 순서 보장.
    @Query("SELECT o FROM Outbox o WHERE o.status IN (:statuses) ORDER BY o.createdAt ASC")
    List<Outbox> findByStatuses(List<OutboxStatus> statuses);
}
