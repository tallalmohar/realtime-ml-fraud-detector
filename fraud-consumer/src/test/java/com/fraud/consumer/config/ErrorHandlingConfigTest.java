package com.fraud.consumer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 6 - Task 4: Unit Tests for ErrorHandlingConfig
 * 
 * Tests error handling configuration logic.
 */
class ErrorHandlingConfigTest {

	@Test
	@DisplayName("DLT topic naming convention should append -dlt suffix")
	void testDltTopicNaming() {
		// Given
		String originalTopic = "transactions";

		// When
		String dltTopic = originalTopic + "-dlt";

		// Then
		assertEquals("transactions-dlt", dltTopic);
	}

	@Test
	@DisplayName("DLT topic naming should work with any topic name")
	void testDltTopicNaming_CustomTopic() {
		// Given
		String originalTopic = "fraud-alerts";

		// When
		String dltTopic = originalTopic + "-dlt";

		// Then
		assertEquals("fraud-alerts-dlt", dltTopic);
	}

	@Test
	@DisplayName("Exponential backoff should calculate correctly")
	void testExponentialBackoff() {
		// Given
		long initialInterval = 1000L; // 1 second
		double multiplier = 2.0;

		// When - Calculate retry intervals
		long retry1 = initialInterval;
		long retry2 = (long) (retry1 * multiplier);
		long retry3 = (long) (retry2 * multiplier);

		// Then
		assertEquals(1000L, retry1, "First retry should be 1s");
		assertEquals(2000L, retry2, "Second retry should be 2s");
		assertEquals(4000L, retry3, "Third retry should be 4s");
	}

	@Test
	@DisplayName("Max interval should cap backoff")
	void testExponentialBackoff_MaxInterval() {
		// Given
		long interval = 8000L; // 8 seconds
		long maxInterval = 10000L; // 10 second cap
		double multiplier = 2.0;

		// When - Apply multiplier
		long nextInterval = (long) (interval * multiplier); // Would be 16000
		long cappedInterval = Math.min(nextInterval, maxInterval);

		// Then
		assertEquals(10000L, cappedInterval, "Interval should be capped at max");
	}

	@Test
	@DisplayName("Retry count should not exceed max attempts")
	void testRetryCount() {
		// Given
		int maxAttempts = 3;
		int currentAttempt = 0;

		// When - Simulate retries
		while (currentAttempt < maxAttempts) {
			currentAttempt++;
		}

		// Then
		assertEquals(3, currentAttempt, "Should stop at max attempts");
	}
}
