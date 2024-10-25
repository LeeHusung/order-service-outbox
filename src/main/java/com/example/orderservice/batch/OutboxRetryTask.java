//package com.example.orderservice.jpa;
//
//import com.example.orderservice.messagequeue.KafkaProducer;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//@Slf4j
//public record OutboxRetryTask(OutboxRepository outboxRepository,
//                              KafkaProducer kafkaProducer,
//                              ObjectMapper objectMapper) {
//
//    @Scheduled(fixedDelayString = "10000")
//    public void retry() {
//        List<Outbox> outboxEntities = outboxRepository.findAllBefore(LocalDateTime.now().minusSeconds(60));
//        for (Outbox outbox : outboxEntities) {
//            log.info("try retry outbox Id = {}", outbox.getId());
//            kafkaProducer.send("example-catalog-topic", outbox);
//            outboxRepository.delete(outbox);
//        }
//    }
//}
