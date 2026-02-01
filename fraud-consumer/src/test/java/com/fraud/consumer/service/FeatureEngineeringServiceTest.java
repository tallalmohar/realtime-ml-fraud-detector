package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 6 - Task 4: Unit Tests for FeatureEngineeringService
 * 
 * Tests the ML feature extraction logic to ensure:
 * - Correct feature count (30 features)
 * - Feature order: Time (0), V1-V28 (1-28), Amount (29)
 * - Correct extraction of values
 */
class FeatureEngineeringServiceTest {

	private FeatureEngineeringService featureService;

	@BeforeEach
	void setUp() {
		featureService = new FeatureEngineeringService();
	}

	@Test
	@DisplayName("Should extract exactly 30 features from transaction")
	void testExtractFeatures_ReturnsCorrectLength() {
		// Given
		Transaction transaction = createTestTransaction();

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then
		assertNotNull(features, "Features array should not be null");
		assertEquals(30, features.length, "Should extract exactly 30 features");
	}

	@Test
	@DisplayName("Should extract amount at index 29")
	void testExtractFeatures_ExtractsAmount() {
		// Given
		Transaction transaction = createTestTransaction();
		transaction.setAmount(new BigDecimal("500.0")); // $500

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then - Amount is at index 29 (last feature)
		assertEquals(500.0f, features[29], 0.001f, "Amount should be at index 29");
	}

	@Test
	@DisplayName("Should extract V1-V28 values at indices 1-28")
	void testExtractFeatures_ExtractsVValues() {
		// Given
		Transaction transaction = createTestTransaction();
		transaction.setV1(1.5f);
		transaction.setV2(2.5f);
		transaction.setV3(3.5f);
		transaction.setV28(-0.5f);

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then - V1-V28 are at indices 1-28
		assertEquals(1.5f, features[1], 0.001f, "V1 should be at index 1");
		assertEquals(2.5f, features[2], 0.001f, "V2 should be at index 2");
		assertEquals(3.5f, features[3], 0.001f, "V3 should be at index 3");
		assertEquals(-0.5f, features[28], 0.001f, "V28 should be at index 28");
	}

	@Test
	@DisplayName("Should handle zero amount")
	void testExtractFeatures_HandlesZeroAmount() {
		// Given
		Transaction transaction = createTestTransaction();
		transaction.setAmount(BigDecimal.ZERO);

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then
		assertEquals(0.0f, features[29], 0.001f, "Zero amount should be at index 29");
	}

	@Test
	@DisplayName("Should handle large amount values")
	void testExtractFeatures_HandlesLargeAmount() {
		// Given
		Transaction transaction = createTestTransaction();
		transaction.setAmount(new BigDecimal("10000.0")); // $10,000

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then - Amount is raw value at index 29
		assertEquals(10000.0f, features[29], 0.001f, "Large amount should be at index 29");
	}

	@Test
	@DisplayName("Should handle negative V values")
	void testExtractFeatures_HandlesNegativeVValues() {
		// Given
		Transaction transaction = createTestTransaction();
		transaction.setV1(-2.5f);
		transaction.setV14(-3.7f);

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then - V1 at index 1, V14 at index 14
		assertEquals(-2.5f, features[1], 0.001f, "Should preserve negative V1 at index 1");
		assertEquals(-3.7f, features[14], 0.001f, "Should preserve negative V14 at index 14");
	}

	@Test
	@DisplayName("Should extract Time at index 0")
	void testExtractFeatures_ExtractsTime() {
		// Given
		Transaction transaction = createTestTransaction();
		transaction.setTime(43200.0f); // Seconds since first transaction

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then - Time is at index 0
		assertEquals(43200.0f, features[0], 0.001f, "Time should be at index 0");
	}

	@Test
	@DisplayName("Should handle all zero V values")
	void testExtractFeatures_HandlesAllZeroVValues() {
		// Given
		Transaction transaction = createTestTransaction();
		// V1-V28 default to 0.0 in test transaction

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then - All V values (indices 1-28) should be 0
		for (int i = 1; i <= 28; i++) {
			assertEquals(0.0f, features[i], 0.001f, "V" + i + " should be 0 at index " + i);
		}
	}

	@Test
	@DisplayName("Should handle null V values gracefully")
	void testExtractFeatures_HandlesNullVValues() {
		// Given
		Transaction transaction = new Transaction();
		transaction.setTransactionID("test-txn-001");
		transaction.setAmount(new BigDecimal("100.0"));
		// V values are null

		// When
		float[] features = featureService.extractFeatures(transaction);

		// Then - Null V values should default to 0
		for (int i = 1; i <= 28; i++) {
			assertEquals(0.0f, features[i], 0.001f, "Null V" + i + " should default to 0");
		}
	}

	// Helper method to create a test transaction with default values
	private Transaction createTestTransaction() {
		Transaction t = new Transaction();
		t.setTransactionID("test-txn-001");
		t.setUserId("user-001");
		t.setMerchantId("merchant-001");
		t.setAmount(new BigDecimal("100.0"));
		t.setTimestamp(LocalDateTime.now());
		t.setLocation("Test City");
		t.setPaymentMethod("CREDIT_CARD");
		t.setTime(0f);

		// Initialize V1-V28 to 0 (will be overridden in specific tests)
		t.setV1(0f);
		t.setV2(0f);
		t.setV3(0f);
		t.setV4(0f);
		t.setV5(0f);
		t.setV6(0f);
		t.setV7(0f);
		t.setV8(0f);
		t.setV9(0f);
		t.setV10(0f);
		t.setV11(0f);
		t.setV12(0f);
		t.setV13(0f);
		t.setV14(0f);
		t.setV15(0f);
		t.setV16(0f);
		t.setV17(0f);
		t.setV18(0f);
		t.setV19(0f);
		t.setV20(0f);
		t.setV21(0f);
		t.setV22(0f);
		t.setV23(0f);
		t.setV24(0f);
		t.setV25(0f);
		t.setV26(0f);
		t.setV27(0f);
		t.setV28(0f);

		return t;
	}
}
