package com.example.orderservice.messagequeue;

import com.example.orderservice.top.domain.Outbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaProducer {
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
//
//    public OrderDto send(String topic, OrderDto orderDto) {
//        ObjectMapper mapper = new ObjectMapper();
//        String jsonInString = "";
//        try {
//            jsonInString = mapper.writeValueAsString(orderDto);
//        } catch (JsonProcessingException ex) {
//            ex.printStackTrace();
//        }
//        System.out.println(jsonInString);
//
//        //여기 탈 때 무한 로딩걸림. 밑밑 로그 안찍힘.
//        kafkaTemplate.send(topic, jsonInString);
//        log.info("Kafka Producer sent data from the Order microservice: " + orderDto);
//
//        return orderDto;
//    }

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
