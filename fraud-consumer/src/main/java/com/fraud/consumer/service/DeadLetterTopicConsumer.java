package com.fraud.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Sprint 6 - Task 1: Dead Letter Topic Consumer
 * 
 * Listens to the Dead Letter Topic (transactions-dlt) to monitor and log
 * messages that failed processing after all retry attempts.
 */
@Service
@Slf4j
public class DeadLetterTopicConsumer {

    @KafkaListener(topics = "transactions-dlt", groupId = "fraud-dlt-monitoring-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDeadLetterMessage(
            @Payload String messagePayload,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
            @Header(value = KafkaHeaders.RECEIVED_TIMESTAMP, required = false) Long timestamp,
            @Header(value = "kafka_dlt-original-topic", required = false) String originalTopic,
            @Header(value = "kafka_dlt-exception-message", required = false) String exceptionMessage,
            @Header(value = "kafka_dlt-exception-fqcn", required = false) String exceptionClass) {

        log.error("═══════════════════════════════════════════════════════════════");
        log.error("⚠️  DEAD LETTER MESSAGE RECEIVED - INVESTIGATION REQUIRED");
        log.error("═══════════════════════════════════════════════════════════════");
        log.error("📍 DLT Topic: {}", topic);
        log.error("📍 DLT Partition: {}", partition);
        log.error("📍 DLT Offset: {}", offset);
        log.error("📍 DLT Timestamp: {}", timestamp != null ? new java.util.Date(timestamp) : "N/A");
        log.error("───────────────────────────────────────────────────────────────");
        log.error("🔙 Original Topic: {}", originalTopic != null ? originalTopic : "transactions");
        log.error("❌ Error Type: {}", exceptionClass != null ? exceptionClass : "Unknown");
        log.error("❌ Error Message: {}", exceptionMessage != null ? exceptionMessage : "No error message");
        log.error("───────────────────────────────────────────────────────────────");
        log.error("📄 Message Payload (first 500 chars):");
        log.error("{}", messagePayload != null && messagePayload.length() > 500
                ? messagePayload.substring(0, 500) + "..."
                : messagePayload);
        log.error("═══════════════════════════════════════════════════════════════");

    }
}
