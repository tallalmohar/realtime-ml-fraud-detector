package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionConsumer {

    @KafkaListener(topics = "transactions", groupId = "fraud-detection-group")
    public void consumeTransaction(Transaction transaction) {
        log.info("📨 Received transaction: ID={}, Amount={}, Merchant={}, Location={}, PaymentMethod={}", 
                transaction.getTransactionID(), 
                transaction.getAmount(), 
                transaction.getMerchantId(),
                transaction.getLocation(),
                transaction.getPaymentMethod());
    }
}
