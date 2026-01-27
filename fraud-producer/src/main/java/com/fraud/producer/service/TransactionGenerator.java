package com.fraud.producer.service;

import com.fraud.common.model.Transaction;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionGenerator {

    public Transaction generateTransaction(){
        Faker faker = new Faker();
        Transaction generatedTransaction = new Transaction();
        generatedTransaction.setTransactionID(UUID.randomUUID().toString());
        generatedTransaction.setUserId(faker.finance().creditCard());
        generatedTransaction.setAmount(BigDecimal.valueOf(faker
                .number()
                .randomDouble(2,1,1000)));
        generatedTransaction.setMerchantId(faker.company().name());
        generatedTransaction.setTimestamp(LocalDateTime.now());
        generatedTransaction.setLocation(faker.address().city() + ", " + faker.address().country());
        generatedTransaction.setPaymentMethod(faker.options().option("CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "BANK_TRANSFER", "CRYPTO"));
        return generatedTransaction;
    }
}
