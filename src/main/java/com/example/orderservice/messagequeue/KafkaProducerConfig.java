package com.example.orderservice.messagequeue;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        //** 지금은 우선 serviceImpl에서 동기로 한 tx로 묶어서 진행했을 때를 비교하는 것임.
        //1분 지나기 전에 카프카 되살리면 동기로 했을 때 응답이 그때 오면서 재고도 감소함. 즉, 재시도가 성공함.
        //만약 1분이 지나고 카프카가 살아나면? 재시도가 성공할까? 2분 지나고 카프카를 되살려보니, 똑같이 재시도가 성공함. order도 저장이 되고 catalog도 감소함.
        //더 길게 3분이 지나고 카프카 복구해보자. -> 성공함. max_block 시간 안에만 복구하면 재시도가 성공함.
        //그러면 DELIVERY_TIMEOUT_MS_CONFIG 이 설정은 동기식에서는 사용안하나?
        //비동기에서 테스트해보자. 뭐지? 비동기로 해도 DELIVERY_TIMEOUT_MS_CONFIG 이 시간이 지나도 에러가 안남. 한 tx로 묶은 상태라 그런가?
        //비동기에서 MAX_BLOCK_MS_CONFIG를 1분으로 설정하니 1분 지나고 에러가 발생함. 즉, MAX_BLOCK_MS_CONFIG 이 설정에만 영향을 받음.
        //하지만 비동기로 하고 1분 지나서 kafka 복구하면 order는 저장이 됨. but, 메시지가 유실되고 재고는 감소가 안됨.
        //비동기로 하고 MAX_BLOCK_MS_CONFIG를 5분 설정하면 5분 안에만 카프카 복구하면 메시지 재발행이 됨.


        //기존 코드 = 즉, 컨트롤러에서 createOrder와 sendKafka 각각 호출 = 하나의 tx로 안 묶인 코드로 테스트하면,
        //order는 저장이 된다. 그리고 DELIVERY_TIMEOUT_MS_CONFIG가 30초였는데 30초가 지나도 실패하지 않고 이후 복구하면 메시지 발행 됨.
        //MAX_BLOCK_MS_CONFIG를 1분으로 설정하면, 1분 뒤에 에러 나온다. 즉, 기존 코드도 MAX_BLOCK_MS_CONFIG가 적용된다.
        //물론 에러가 터지면 메시지는 유실된다. (주문은 저장됨)

        //정리하자면, kafka가 down 상황에서,
        // kafka.send()를 비동기를 적용한다면 주문은 무조건 저장이 된다. (tx를 하나로 묶든 안묶든.)
        //하지만 kafka.send()와 saveOrder()를 하나의 tx로 묶고, send()를 동기로 하면 주문을 할 때, 로딩이 걸린다. send() 메서드의 응답을 받아야 하는데,
        //kafkaTemplate.send()가 계속 실패해서 계속 기다리기 때문임. 이때 MAX_BLOCK_MS_CONFIG 시간이 지나면 에러가 반환된다. 그리고 롤백이 되므로
        //order도 저장이 되지 않는다. 하나의 tx로 묶으면 kafka.send()에서 에러가 터지면 saveOrder()도 롤백된다. 하지만 로딩을 기다려야 함.

        //tx를 분리하면,
        //비동기를 적용하면, 역시 order는 저장이 되고 메시지 발행은 실패한다. 그리고 MAX_BLOCK_MS_CONFIG 이 값이 지나면 에러가 발생하고 메시지가 유실된다.
        //동기를 적용하면, order는 저장이 된다. 하지만 로딩이 걸림. 그리고 MAX_BLOCK_MS_CONFIG 이 값이 지나면 에러가 발생하고 메시지가 유실된다.
        //에러 메시지는 블로그에.

        //MAX_BLOCK_MS_CONFIG 이거 설정 없애고 하면,
        //2024-10-30T13:19:28.605+09:00 ERROR 16404 --- [order-service] [vice-producer-1] ERROR [order-service,,]o.s.k.support.LoggingProducerListener    : Exception thrown when sending a message with key='null' and payload='{"id":null,"productId":"CATALOG-002","qty":10,"unitPrice":1500,"totalPrice":15000,"orderId":"f2fbeef...' to topic catalog-topic:
        //org.apache.kafka.common.errors.TimeoutException: Expiring 1 record(s) for catalog-topic-0:30006 ms has passed since batch creation
        //위 에러가 나온다. DELIVERY_TIMEOUT_MS_CONFIG 이 값 이후에. 물론 메시지는 유실됨

        // DELIVERY_TIMEOUT_MS_CONFIG vs MAX_BLOCK_MS_CONFIG
        //kafka가 끊긴 경우에는 서버에서는 먼저 kafka 자원을 기다려야 한다.
        //메시지를 보내기 위해 필요한 자원을 기다리는 최대 시간이 MAX_BLOCK_MS_CONFIG 이다.
        //DELIVERY_TIMEOUT_MS_CONFIG는 메시지를 브로커에 최종적으로 보내기 위해 기다리는 전체 시간이다.
        //즉, kafka가 down되고 order를 수행하면 재고 감소 메시지를 보내기 위해 카프카 자원을 기다리게 된다. 이때 MAX_BLOCK_MS_CONFIG 이게 쓰인다.
        //자원을 확보하지 못하면 MAX_BLOCK_MS_CONFIG 이게 해당되고,
        //만약 자원을 확보했지만 다른 원인으로 카프카에 메시지 발행을 못하면 DELIVERY_TIMEOUT_MS_CONFIG 시간만큼 기다렸다가 에러를 발생시킨다.
        //즉 자원이 확보되지 못했으면 MAX_BLOCK_MS_CONFIG 이걸 먼저 탄다. = 둘 다 설정하면 MAX_BLOCK_MS_CONFIG 시간만큼 기다림.???

        //다시,
        //현재 orderController 로 진행중. (가장 기본) 동기식으로. 근데 왜 동기식인데 응답이 바로 오지?? 응답은 여전히 바로 온다.
        //어쩔땐 또 로딩이 걸린다.??
        // 서버 키자마자 카프카 죽이고 테스트하면, 로딩걸림. -> 동기식이니까 예상했던 결과임. -> 에러 안나오다가 요청하면 연결안됨 에러 나옴. 로딩걸림.
        //MAX_BLOCK_MS_CONFIG 이후에 에러 발생함.

        // 정상적으로 한 번 동작하고 카프카 죽이고 테스트하면 응답이 바로 옴. 그러나 당연히 재고 감소 안됨. DELIVERY_TIMEOUT_MS_CONFIG 지나면 에러 터짐
        //즉 정상 호출이 한 번 되었으면 스프링이 필요한 자원을 보관하고 있단 것을 알고 있다.
        //동기여도 주문을 정상 처리하고 카프카로 메시지 재시도를 DELIVERY_TIMEOUT_MS_CONFIG 동안 진행하게 됨.

        //둘 다 설정 되어있고, DELIVERY_TIMEOUT_MS_CONFIG 시간 이후에 카프카 복구하면? 30초 지나면 에러가 발생함. = 메시지 유실

        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);  // 30초 지나면 실패하도록 설정 (=메시지 유실됨)
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); //동기로 요청 시 이 시간 동안 요청 기다림. 시간 초과시 에러발생
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);                  // 재시도 횟수 설정
        properties.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 500);       // 재시도 간 대기 시간 설정

        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

}