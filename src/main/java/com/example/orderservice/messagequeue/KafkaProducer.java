package com.example.orderservice.messagequeue;

import com.example.orderservice.domain.OrderEntity;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.top.domain.Outbox;
import com.example.orderservice.top.dto.OrderExternalEventMessagePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@Slf4j
public class KafkaProducer {
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    public void send(String topic, OrderDto orderDto) {
        log.info("kafka send start");
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "";
        try {
            jsonInString = mapper.registerModule(new JavaTimeModule()).writeValueAsString(orderDto);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }
        System.out.println(jsonInString);

        //여기 탈 때 무한 로딩걸림. 밑밑 로그 안찍힘.
        //근데 애초에 아래 메서드가 비동긴데 왜 @Async 안 붙이면 무한로딩이 걸리지?
        //비동기로 하면 바로 재고가 감소가 안됨. 10초는 기다려야 되는듯. 너무 느린데?
        //동기로 하면 바로 됨.
        kafkaTemplate.send(topic, jsonInString);
        log.info("Kafka Producer sent data from the Order microservice: " + orderDto);

    }

//    public Outbox send(String topic, Outbox outbox) {
//        ObjectMapper mapper = new ObjectMapper();
//        String jsonInString = "";
//        try {
//            jsonInString = mapper.writeValueAsString(outbox);
//        } catch (JsonProcessingException ex) {
//            ex.printStackTrace();
//        }
//        System.out.println(jsonInString);
//
//        kafkaTemplate.send(topic, jsonInString);
//        log.info("Kafka Producer sent data from the Order microservice: " + outbox);
//
//        return outbox;
//    }
}
