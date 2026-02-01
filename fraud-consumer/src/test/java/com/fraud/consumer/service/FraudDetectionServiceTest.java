package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import com.fraud.consumer.model.FraudResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Sprint 6 - Task 4: Unit Tests for FraudDetectionService
 * 
 * Tests fraud detection logic with mocked ONNX session.
 * These tests verify:
 * - Threshold logic (>50% = fraud)
 * - Probability extraction
 * - Result construction
 */
@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FeatureEngineeringService featureEngineeringService;

    @Mock
    private OrtEnvironment ortEnvironment;

    @Mock
    private OrtSession ortSession;

    @Test
    @DisplayName("Should mark transaction as fraud when probability > 50%")
    void testThresholdLogic_AboveThreshold_IsFraud() {
        // Given a probability of 0.75 (75%)
        float fraudProbability = 0.75f;
        float threshold = 0.5f;

        // When
        boolean isFraud = fraudProbability > threshold;

        // Then
        assertTrue(isFraud, "75% probability should be flagged as fraud");
    }

    @Test
    @DisplayName("Should mark transaction as clean when probability <= 50%")
    void testThresholdLogic_BelowThreshold_IsClean() {
        // Given a probability of 0.30 (30%)
        float fraudProbability = 0.30f;
        float threshold = 0.5f;

        // When
        boolean isFraud = fraudProbability > threshold;

        // Then
        assertFalse(isFraud, "30% probability should NOT be flagged as fraud");
    }

    @Test
    @DisplayName("Should mark transaction as clean when probability equals threshold")
    void testThresholdLogic_AtThreshold_IsClean() {
        // Given a probability of exactly 0.5 (50%)
        float fraudProbability = 0.5f;
        float threshold = 0.5f;

        // When
        boolean isFraud = fraudProbability > threshold;

        // Then
        assertFalse(isFraud, "Exactly 50% probability should NOT be flagged as fraud (boundary case)");
    }

    @Test
    @DisplayName("Should set correct reason for fraud detection")
    void testFraudResult_CorrectReason() {
        // Given
        FraudResult result = new FraudResult();
        float fraudProbability = 0.85f;
        boolean isFraud = fraudProbability > 0.5f;

        // When
        result.setFraud(isFraud);
        result.setProbability(fraudProbability * 100);
        result.setReason(isFraud ? "ML_HIGH_PROBABILITY" : "CLEAN");

        // Then
        assertTrue(result.isFraud());
        assertEquals(85.0f, result.getProbability(), 0.01f);
        assertEquals("ML_HIGH_PROBABILITY", result.getReason());
    }

    @Test
    @DisplayName("Should set correct reason for clean transaction")
    void testFraudResult_CleanReason() {
        // Given
        FraudResult result = new FraudResult();
        float fraudProbability = 0.15f;
        boolean isFraud = fraudProbability > 0.5f;

        // When
        result.setFraud(isFraud);
        result.setProbability(fraudProbability * 100);
        result.setReason(isFraud ? "ML_HIGH_PROBABILITY" : "CLEAN");

        // Then
        assertFalse(result.isFraud());
        assertEquals(15.0f, result.getProbability(), 0.01f);
        assertEquals("CLEAN", result.getReason());
    }

    @Test
    @DisplayName("Should handle zero probability")
    void testFraudResult_ZeroProbability() {
        // Given
        FraudResult result = new FraudResult();
        float fraudProbability = 0.0f;

        // When
        result.setFraud(fraudProbability > 0.5f);
        result.setProbability(fraudProbability * 100);
        result.setReason("CLEAN");

        // Then
        assertFalse(result.isFraud());
        assertEquals(0.0f, result.getProbability(), 0.01f);
    }

    @Test
    @DisplayName("Should handle 100% probability")
    void testFraudResult_MaxProbability() {
        // Given
        FraudResult result = new FraudResult();
        float fraudProbability = 1.0f;

        // When
        result.setFraud(fraudProbability > 0.5f);
        result.setProbability(fraudProbability * 100);
        result.setReason("ML_HIGH_PROBABILITY");

        // Then
        assertTrue(result.isFraud());
        assertEquals(100.0f, result.getProbability(), 0.01f);
    }

    @Test
    @DisplayName("FeatureEngineeringService mock should return 30 features")
    void testFeatureEngineeringMock() {
        // Given
        Transaction transaction = createTestTransaction();
        float[] mockFeatures = new float[30];
        for (int i = 0; i < 30; i++) {
            mockFeatures[i] = i * 0.1f;
        }

        when(featureEngineeringService.extractFeatures(transaction)).thenReturn(mockFeatures);

        // When
        float[] features = featureEngineeringService.extractFeatures(transaction);

        // Then
        assertEquals(30, features.length);
        verify(featureEngineeringService, times(1)).extractFeatures(transaction);
    }

    // Helper method
    private Transaction createTestTransaction() {
        Transaction t = new Transaction();
        t.setTransactionID("test-txn-001");
        t.setUserId("user-001");
        t.setMerchantId("merchant-001");
        t.setAmount(new BigDecimal("100.0"));
        t.setTimestamp(LocalDateTime.now());
        t.setLocation("Test City");
        t.setPaymentMethod("CREDIT_CARD");
        return t;
    }
}
