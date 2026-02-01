package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import com.fraud.consumer.entity.FraudulentTransaction;
import com.fraud.consumer.model.FraudResult;
import com.fraud.consumer.repository.FraudulentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Sprint 6 - Task 2: Added @Retryable for database resilience
 */
@Slf4j
@Service
public class FraudPersistenceService {
    private FraudulentTransactionRepository fraudulentTransactionRepository;

    public FraudPersistenceService(FraudulentTransactionRepository fraudulentTransactionRepository) {
        this.fraudulentTransactionRepository = fraudulentTransactionRepository;
    }

    /**
     * Saves fraudulent transaction to database with retry logic.
     * 
     * Retry policy:
     * - Max attempts: 3
     * - Backoff: Exponential (1s, 2s, 4s)
     * - Retryable on: DataAccessException (DB connection issues, timeouts)
     */
    @Retryable(retryFor = {
            DataAccessException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000))
    public void saveFraudulentTransaction(Transaction transaction, FraudResult fraudResult) {
        log.debug("Attempting to save fraud: TransactionID={}", transaction.getTransactionID());

        FraudulentTransaction fraud = new FraudulentTransaction();
        fraud.setDetectionMethod("ML_MODEL");
        fraud.setFraudProbability(fraudResult.getProbability());
        fraud.setTransactionID(transaction.getTransactionID());
        fraud.setUserId(transaction.getUserId());
        fraud.setAmount(transaction.getAmount());
        fraud.setMerchantId(transaction.getMerchantId());
        fraud.setTimestamp(transaction.getTimestamp());
        fraud.setLocation(transaction.getLocation());
        fraud.setPaymentMethod(transaction.getPaymentMethod());

        fraud.setFlagReason(fraudResult.getReason());
        fraud.setDetectedAt(LocalDateTime.now());

        fraudulentTransactionRepository.save(fraud);
        log.info("Fraud saved: TransactionID={}, Reason={}",
                fraud.getTransactionID(),
                fraud.getFlagReason());
    }

    /**
     * Recovery method called after all retries are exhausted.
     * Logs the failure for manual intervention.
     */
    @Recover
    public void recoverFromSaveFailure(DataAccessException e, Transaction transaction, FraudResult fraudResult) {
        log.error("❌ CRITICAL: Failed to save fraud after 3 retries!");
        log.error("TransactionID: {}", transaction.getTransactionID());
        log.error("Amount: ${}", transaction.getAmount());
        log.error("Fraud Probability: {}%", fraudResult.getProbability());
        log.error("Error: {}", e.getMessage());
        log.error("⚠️ Manual intervention required - fraud record lost!");

        // TODO (Production):
        // 1. Send to dead letter topic for fraud records
        // 2. Alert PagerDuty
        // 3. Write to local file as backup
        // 4. Increment Prometheus counter: fraud_save_failures_total
    }
}
