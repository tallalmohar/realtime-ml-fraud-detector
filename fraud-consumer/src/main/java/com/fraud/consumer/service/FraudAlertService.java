package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FraudAlertService {
    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    public FraudAlertService(KafkaTemplate<String,Transaction> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendFraudAlert(Transaction transaction){
        kafkaTemplate.send("fraud-alerts",transaction.getTransactionID(),transaction);
        log.warn("Fraud alert sent to Kafka: TransactionID={}, Amount=${}",
                transaction.getTransactionID(),
                transaction.getAmount());
    }


}
