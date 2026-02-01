package com.fraud.consumer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Sprint 6 - Task 2: Retry Configuration
 * 
 * Enables Spring Retry for the application, allowing methods annotated
 * with @Retryable to automatically retry on failure.
 * 
 * Usage: Add @Retryable to any method that may fail transiently
 * Example: Database operations, external API calls
 */
@Slf4j
@Configuration
@EnableRetry
public class RetryConfig {

	// @EnableRetry activates retry processing for the entire application
	// Individual retry policies are configured via @Retryable annotations
	//
	// Default retry behavior (can be overridden per method):
	// - Max attempts: 3
	// - Backoff: Exponential (1s, 2s, 4s)
	// - Retryable exceptions: All exceptions by default

	// Note: For more complex retry scenarios, you can create a RetryTemplate bean:
	//
	// @Bean
	// public RetryTemplate retryTemplate() {
	// RetryTemplate template = new RetryTemplate();
	//
	// ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
	// backOff.setInitialInterval(1000);
	// backOff.setMultiplier(2.0);
	// backOff.setMaxInterval(10000);
	// template.setBackOffPolicy(backOff);
	//
	// SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
	// retryPolicy.setMaxAttempts(3);
	// template.setRetryPolicy(retryPolicy);
	//
	// return template;
	// }
}
