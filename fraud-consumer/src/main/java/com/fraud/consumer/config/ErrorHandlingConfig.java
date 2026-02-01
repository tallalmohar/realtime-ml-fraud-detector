package com.fraud.consumer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.TopicPartitionOffset;

@Configuration
@Slf4j
public class ErrorHandlingConfig {

	@Bean
	@ConditionalOnBean(KafkaTemplate.class)
	public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {

		// Step 3a: Create DeadLetterPublishingRecoverer
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
				kafkaTemplate,
				(consumerRecord, exception) -> {
					// Build DLT topic name
					String dltTopic = consumerRecord.topic() + "-dlt";

					// Log the failure
					log.error("⚠️ Message failed after 3 retries. Routing to DLT: {} | Key: {} | Error: {}",
							dltTopic,
							consumerRecord.key(),
							exception.getMessage());

					// Return where to send the failed message
					return new TopicPartitionOffset(dltTopic, 0, 0L).getTopicPartition();
				});

		// Step 3b: Create ExponentialBackOffWithMaxRetries
		ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
		backOff.setInitialInterval(1000L); // 1 second
		backOff.setMultiplier(2.0); // Double each time
		backOff.setMaxInterval(10000L); // Cap at 10 seconds

		// Step 3c: Create DefaultErrorHandler
		DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

		// Add retry listener to log each retry attempt
		errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
			log.warn("🔄 Retry attempt {}/3 for message: Key={}, Offset={}, Error={}",
					deliveryAttempt,
					record.key(),
					record.offset(),
					ex.getMessage());
		});

		// Add non-retryable exceptions (go straight to DLT)
		errorHandler.addNotRetryableExceptions(
				org.springframework.kafka.support.serializer.DeserializationException.class,
				com.fasterxml.jackson.core.JsonParseException.class,
				com.fasterxml.jackson.databind.JsonMappingException.class);

		log.info("✅ Kafka Error Handler configured: 3 retries with exponential backoff → DLT");

		return errorHandler;
	}
}
