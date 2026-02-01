package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FeatureEngineeringService {

    /**
     * Extract 30 features in exact order expected by ONNX model:
     * Features 0-29: Time, V1-V28, Amount
     */
    public float[] extractFeatures(Transaction transaction) {
        float[] features = new float[30];

        // Feature 0: Time (seconds since first transaction)
        features[0] = transaction.getTime() != null ? transaction.getTime() : 0.0f;

        // Features 1-28: V1-V28 (PCA-transformed features)
        features[1] = transaction.getV1() != null ? transaction.getV1() : 0.0f;
        features[2] = transaction.getV2() != null ? transaction.getV2() : 0.0f;
        features[3] = transaction.getV3() != null ? transaction.getV3() : 0.0f;
        features[4] = transaction.getV4() != null ? transaction.getV4() : 0.0f;
        features[5] = transaction.getV5() != null ? transaction.getV5() : 0.0f;
        features[6] = transaction.getV6() != null ? transaction.getV6() : 0.0f;
        features[7] = transaction.getV7() != null ? transaction.getV7() : 0.0f;
        features[8] = transaction.getV8() != null ? transaction.getV8() : 0.0f;
        features[9] = transaction.getV9() != null ? transaction.getV9() : 0.0f;
        features[10] = transaction.getV10() != null ? transaction.getV10() : 0.0f;
        features[11] = transaction.getV11() != null ? transaction.getV11() : 0.0f;
        features[12] = transaction.getV12() != null ? transaction.getV12() : 0.0f;
        features[13] = transaction.getV13() != null ? transaction.getV13() : 0.0f;
        features[14] = transaction.getV14() != null ? transaction.getV14() : 0.0f;
        features[15] = transaction.getV15() != null ? transaction.getV15() : 0.0f;
        features[16] = transaction.getV16() != null ? transaction.getV16() : 0.0f;
        features[17] = transaction.getV17() != null ? transaction.getV17() : 0.0f;
        features[18] = transaction.getV18() != null ? transaction.getV18() : 0.0f;
        features[19] = transaction.getV19() != null ? transaction.getV19() : 0.0f;
        features[20] = transaction.getV20() != null ? transaction.getV20() : 0.0f;
        features[21] = transaction.getV21() != null ? transaction.getV21() : 0.0f;
        features[22] = transaction.getV22() != null ? transaction.getV22() : 0.0f;
        features[23] = transaction.getV23() != null ? transaction.getV23() : 0.0f;
        features[24] = transaction.getV24() != null ? transaction.getV24() : 0.0f;
        features[25] = transaction.getV25() != null ? transaction.getV25() : 0.0f;
        features[26] = transaction.getV26() != null ? transaction.getV26() : 0.0f;
        features[27] = transaction.getV27() != null ? transaction.getV27() : 0.0f;
        features[28] = transaction.getV28() != null ? transaction.getV28() : 0.0f;

        // Feature 29: Amount
        features[29] = transaction.getAmount().floatValue();

        log.debug("Extracted 30 features for transaction {}: Time={}, Amount={}, V1={}, ...",
                transaction.getTransactionID(), features[0], features[29], features[1]);

        return features;
    }
}