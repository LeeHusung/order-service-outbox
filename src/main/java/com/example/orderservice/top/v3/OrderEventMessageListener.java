package com.example.orderservice.top.v3;

import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.domain.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

//@Component
@Slf4j
@Transactional
public class OrderEventMessageListener {

    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    public OrderEventMessageListener(@Value("${kafka.outbox.topic}") String topic, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, OutboxRepository outboxRepository) {
        this.topic = topic;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }

    //이 비동기 메서드를 실행하기 전에 서버가 죽으면 init으로 남음. = 정상
    //이 비동기 메서드를 실행하다가 kafka가 에러 발생함. send_fail로 변경됨 = 정상
    //실행하다가 kafka로 메시지 발행 전에 서버가 죽음. init으로 남음 = 정상
    //실행하다가 kafka로 메시지 발행 성공 = send_success로 변경됨 = 정상
    //결국 이 비동기 메서드를 모두 실행하기 전에 서버가 죽으면 init으로 남는 상황을 방지하기 위해
    // 서버가 죽을 때 기존 실행중인 비동기 메서드를 모두 실행 완료할 때까지 설정함으로써 init을 없앨 수 있음.
    //but 양날의 검이다. 급하게 죽어야 하는데, 이 설정으로 인해 계속 살아있을 수 있음.

    //init으로 남아있으면 이 listener 문제일 가능성이 큼. kafka로 send() 도 못한거니, 이 메서드가 실행이 안되었을 것이다.
    //send_fail이라면 kafka로 보내지긴 했는데, 실패했단 것임.
    @Async
    @TransactionalEventListener //after commit이란 말은, 이미 영속성 컨텍스트에서 flush 된 후, 여기는 다시 시작하는 거다.
    public void handler(OrderExternalEventMessagePayload payload) {
        log.info("orderDto: {}", payload.toString());
        log.info("OrderEventMessageListener Task executed");

        Outbox outbox = outboxRepository.findByOrderId(payload.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Outbox not found for order: " + payload.getOrderId()));
        //kafka down 시에 밑 kafka 전송 실패 에러가 나오고 catch를 타지 않는다. status도 send_fail로 바뀜. @Async로 실행하니 이렇게 되는데?
        //@Async 안할 때는 catch가 타고, hit이 찍히는데 send_fail이 저장이 안됨.
        //동기로 하면 일정 시간 (1분?) 뒤에 KafkaProducerException 에러 터지고 kafka 전송 실패 로그가 나오고 send_fail로 변경됨.
        //그리고 batch에서도 동일한 로그 나옴.
        //비동기로 실행하면, 밑에 try문에서 send() 계속 실행하는 듯. 그러다가 일정 시간 지난 후에 kafka 전송 실패 로그 찍고 send_fail로 변경되는 듯
        //비동기로 실행하면 밑에 catch 문이 잡히네?
        //배치에서도 catch가 잡히네?? 아까는 catch 안잡혔던 것 같은데. 비동기와 동기 차이인가? 바뀐게 그거인거같은데. 확실치 않음

        //동기로 진행하면 우선 로딩이 걸림.
        // WARN 19580 --- [order-service] [vice-producer-1]  WARN [order-service,,]org.apache.kafka.clients.NetworkClient   : [Producer clientId=order-service-producer-1] Connection to node -1 (/127.0.0.1:9092) could not be established. Node may not be available.
        //2024-10-28T16:53:43.651+09:00  WARN 19580 --- [order-service] [vice-producer-1]  WARN [order-service,,]org.apache.kafka.clients.NetworkClient   : [Producer clientId=order-service-producer-1] Bootstrap broker 127.0.0.1:9092 (id: -1 rack: null) disconnected
        //2024-10-28T16:53:44.660+09:00  INFO 19580 --- [order-service] [vice-producer-1]  INFO [order-service,,]org.apache.kafka.clients.NetworkClient   : [Producer clientId=order-service-producer-1] Node -1 disconnected.
        //이 에러가 나오고. order와 outbox는 제대로 저장이 됨.
        //1분 지나면 201 이 나오면서 ok 나옴. 그리고 KafkaException 터지면서 catch문이 실행됨.
        //배치에서도 catch가 실행됨
        //outbox 테이블 status는 init임. <- 왜? changeFail이 왜 안되지?
        //kafka 재실행하면 send_success로 바뀌고 재고 감소 성공함.

        //비동기로 하면 응답이 바로 옴. 201. 마찬가지로 order와 outbox 생성됨.
        //org.apache.kafka.common.errors.TimeoutException: Topic example-catalog-topic not present in metadata after 60000 ms.
        //2024-10-28T16:59:18.780+09:00  INFO 3916 --- [order-service] [        Async-1]  INFO [order-service,,]c.e.o.top.v3.OrderEventMessageListener   : 처리 중 오류 발생:
        // 1분 지나면 timeoutexception 발생하고 catch 문 들어옴. .KafkaException 발생함
        //배치도 1분 지나면 timeoutexception나오고 동일하게 catch 문 들어옴. 스케쥴러 실행되고 정확히 1분 지나서 발생함.
        // 이후 계속 스케쥴러 돌때마다 1분 후에 에러 발생함.
        //status도 send_fail임. <- 언제 바뀌는거지? 에러 발생하면 바뀜.
        //kafka를 다시 살리면 바로 재전송이 됨. 배치에 의해 되는게 아니라 바로 됨. 왜? 애초에 kafka로 메시지 발행이 안된 상태인데,
        //어떻게 kafka를 살리자마자 메시지 발행이 되고 재고도 감소가 되는거지?
        //kafka를 미리 죽이고 서버 실행시켜도 동일함

        //해결해야 할 것 : kafka를 죽이면    // WARN 19580 --- [order-service] [vice-producer-1]  WARN [order-service,,]org.apache.kafka.clients.NetworkClient   : [Producer clientId=order-service-producer-1] Connection to node -1 (/127.0.0.1:9092) could not be established. Node may not be available.
        //        //2024-10-28T16:53:43.651+09:00  WARN 19580 --- [order-service] [vice-producer-1]  WARN [order-service,,]org.apache.kafka.clients.NetworkClient   : [Producer clientId=order-service-producer-1] Bootstrap broker 127.0.0.1:9092 (id: -1 rack: null) disconnected
        //        //2024-10-28T16:53:44.660+09:00  INFO 19580 --- [order-service] [vice-producer-1]  INFO [order-service,,]org.apache.kafka.clients.NetworkClient   : [Producer clientId=order-service-producer-1] Node -1 disconnected.
        // 이 에러가 계속 나옴. 설정을 통해 몇 번 나오면 안나오게 하고 싶은데 해결 못함.


//        ApplicationEventPublisher 를 사용하는 근본적인 이유는, 처리해야 할 도메인 로직과 그 이후에 처리되어야 할 로직을 분리하기 위함이다.
        //도메인 로직은 도메인의 요구사항에만 집중할 수 있음. 그 이외의 것들은 listener에만 추가하면 된다.
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
            log.info("처리 중 오류 발생: ", e);
            outbox.changeFail(outbox);
            System.out.println("hit");
            log.info("outbox.status ={}", outbox.getStatus());
            outboxRepository.save(outbox);
        }
    }
}
