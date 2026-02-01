package com.fraud.producer.service;

import com.fraud.common.model.Transaction;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.time.Duration;

@Service
public class TransactionGenerator {

    private final Random random = new Random();
    private final LocalDateTime startTime = LocalDateTime.now();

    public Transaction generateTransaction() {
        Faker faker = new Faker();
        Transaction generatedTransaction = new Transaction();

        // Basic fields
        generatedTransaction.setTransactionID(UUID.randomUUID().toString());
        generatedTransaction.setUserId(faker.finance().creditCard());
        generatedTransaction.setAmount(BigDecimal.valueOf(faker.number().randomDouble(2, 1, 1000)));
        generatedTransaction.setMerchantId(faker.company().name());
        generatedTransaction.setTimestamp(LocalDateTime.now());
        generatedTransaction.setLocation(faker.address().city() + ", " + faker.address().country());
        generatedTransaction.setPaymentMethod(
                faker.options().option("CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "BANK_TRANSFER", "CRYPTO"));

        // ML Features: Time + V1-V28
        // Time: seconds since system start
        generatedTransaction.setTime((float) Duration.between(startTime, LocalDateTime.now()).getSeconds());

        // V1-V28: Generate realistic PCA-like features based on Kaggle dataset
        // statistics
        // These are normally distributed with specific patterns
        // Fraud transactions have different distributions for certain V features
        boolean isSuspicious = shouldBeSuspicious(generatedTransaction);

        generatedTransaction.setV1(generateFeature(-3, 3, isSuspicious ? -1.5f : 0.0f));
        generatedTransaction.setV2(generateFeature(-3, 3, isSuspicious ? -1.0f : 0.0f));
        generatedTransaction.setV3(generateFeature(-5, 5, isSuspicious ? 2.0f : 0.0f));
        generatedTransaction.setV4(generateFeature(-3, 3, isSuspicious ? 1.5f : 0.0f));
        generatedTransaction.setV5(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV6(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV7(generateFeature(-5, 5, isSuspicious ? -2.0f : 0.0f));
        generatedTransaction.setV8(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV9(generateFeature(-3, 3, isSuspicious ? -1.5f : 0.0f));
        generatedTransaction.setV10(generateFeature(-5, 5, isSuspicious ? -2.5f : 0.0f));
        generatedTransaction.setV11(generateFeature(-3, 3, isSuspicious ? 1.5f : 0.0f));
        generatedTransaction.setV12(generateFeature(-5, 5, isSuspicious ? -2.0f : 0.0f));
        generatedTransaction.setV13(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV14(generateFeature(-5, 5, isSuspicious ? -3.0f : 0.0f));
        generatedTransaction.setV15(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV16(generateFeature(-5, 5, isSuspicious ? -2.5f : 0.0f));
        generatedTransaction.setV17(generateFeature(-5, 5, isSuspicious ? -3.0f : 0.0f));
        generatedTransaction.setV18(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV19(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV20(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV21(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV22(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV23(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV24(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV25(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV26(generateFeature(-3, 3, isSuspicious ? 0.5f : 0.0f));
        generatedTransaction.setV27(generateFeature(-3, 3, 0.0f));
        generatedTransaction.setV28(generateFeature(-3, 3, 0.0f));

        return generatedTransaction;
    }

    /**
     * Determine if transaction should have suspicious patterns.
     * Based on high amount, CRYPTO payment, or random chance (3%)
     */
    private boolean shouldBeSuspicious(Transaction transaction) {
        double amount = transaction.getAmount().doubleValue();
        String payment = transaction.getPaymentMethod();

        // High amounts or CRYPTO payments are more suspicious
        if (amount > 800 || "CRYPTO".equals(payment)) {
            return true;
        }

        // Random 3% of transactions are suspicious
        return random.nextDouble() < 0.03;
    }

    /**
     * Generate a feature value with normal distribution around a mean.
     * 
     * @param min  Minimum value
     * @param max  Maximum value
     * @param bias Bias towards this value (0 = centered)
     */
    private float generateFeature(float min, float max, float bias) {
        // Generate gaussian (normal) distributed value
        float value = (float) (random.nextGaussian() * 1.0 + bias);

        // Clamp to range
        return Math.max(min, Math.min(max, value));
    }
}
