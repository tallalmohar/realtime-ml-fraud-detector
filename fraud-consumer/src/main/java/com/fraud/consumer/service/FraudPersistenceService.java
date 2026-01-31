package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import com.fraud.consumer.entity.FraudulentTransaction;
import com.fraud.consumer.model.FraudResult;
import com.fraud.consumer.repository.FraudulentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class FraudPersistenceService {
    private FraudulentTransactionRepository fraudulentTransactionRepository;

    public FraudPersistenceService(FraudulentTransactionRepository fraudulentTransactionRepository) {
        this.fraudulentTransactionRepository = fraudulentTransactionRepository;
    }

    // turns the kafka transaction object into db entity
    public void saveFraudulentTransaction(Transaction transaction, FraudResult fraudResult) {
        FraudulentTransaction fraud = new FraudulentTransaction();
        fraud.setDetectionMethod("RULE_BASED"); // will be switched at sprint 5
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
}
