package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import com.fraud.consumer.model.FraudResult;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionConsumer {

    private final FraudDetectionService fraudDetectionService;
    private final FraudPersistenceService fraudPersistenceService;
    private final FraudAlertService fraudAlertService;
    private final MetricsService metricsService;
    public TransactionConsumer(FraudDetectionService fraudDetectionService,
            FraudPersistenceService fraudPersistenceService, FraudAlertService fraudAlertService,
                               MetricsService metricsService) {
        this.fraudDetectionService = fraudDetectionService;
        this.fraudPersistenceService = fraudPersistenceService;
        this.fraudAlertService = fraudAlertService;
        this.metricsService = metricsService;
    }

    @KafkaListener(topics = "transactions", groupId = "fraud-detection-group")
    public void consumeTransaction(Transaction transaction) {
        log.info("Received transaction: ID={}, Amount={}, Merchant={}, Location={}, PaymentMethod={}",
                transaction.getTransactionID(),
                transaction.getAmount(),
                transaction.getMerchantId(),
                transaction.getLocation(),
                transaction.getPaymentMethod());

        metricsService.recordTransaction();
        Timer.Sample sample = metricsService.startTimer();
        try {
            FraudResult isFraud = fraudDetectionService.isFraudulent(transaction);
            if (isFraud.isFraud()) {
                log.warn("Fraud DETECTED: Transaction {} | Amount: ${} | Merchant: {} | Payment: {}",
                        transaction.getTransactionID(),
                        transaction.getAmount(),
                        transaction.getMerchantId(),
                        transaction.getPaymentMethod());

                fraudPersistenceService.saveFraudulentTransaction(transaction, isFraud);
                fraudAlertService.sendFraudAlert(transaction);
                metricsService.recordFraud();
            } else {
                log.info("Transaction OK: {} | Amount: ${}",
                        transaction.getTransactionID(),
                        transaction.getAmount());
                metricsService.recordClean();
            }
        } catch (Exception e) {
            log.error("Error processing transaction {}: {}",
                    transaction.getTransactionID(),
                    e.getMessage(),
                    e);
        }finally {
            metricsService.recordDetectionTime(sample);
        }
    }
}
