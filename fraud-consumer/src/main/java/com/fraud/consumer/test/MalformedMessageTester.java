package com.fraud.consumer.test;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Sprint 6 - Task 1: DLT Testing Utility
 * 
 * Sends intentionally malformed messages to test Dead Letter Topic routing.
 * 
 * Usage:
 * 1. Start fraud-consumer in IntelliJ
 * 2. Run this class
 * 3. Watch fraud-consumer logs for:
 * - "🔄 Retry attempt X/3" messages
 * - "⚠️ DEAD LETTER MESSAGE RECEIVED" messages
 * 4. Consumer should NOT crash
 */
public class MalformedMessageTester {

	private static final String KAFKA_BROKER = "localhost:9092";
	private static final String TOPIC = "transactions";

	public static void main(String[] args) throws Exception {

		System.out.println("═══════════════════════════════════════════════════════════════");
		System.out.println("🧪 DLT TEST: Sending malformed messages to Kafka");
		System.out.println("═══════════════════════════════════════════════════════════════");

		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.ACKS_CONFIG, "all");

		try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {

			// Test Case 1: Missing closing bracket
			System.out.println("\n📤 Test 1: Missing closing bracket...");
			String test1 = "{\"transactionID\":\"test-001\",\"amount\":100.50";
			producer.send(new ProducerRecord<>(TOPIC, "test-001", test1));
			Thread.sleep(5000);

			// Test Case 2: Extra comma (invalid JSON)
			System.out.println("\n📤 Test 2: Extra comma...");
			String test2 = "{\"transactionID\":\"test-002\",\"amount\":200.00,}";
			producer.send(new ProducerRecord<>(TOPIC, "test-002", test2));
			Thread.sleep(5000);

			// Test Case 3: Wrong data type
			System.out.println("\n📤 Test 3: Wrong data type (string for amount)...");
			String test3 = "{\"transactionID\":\"test-003\",\"amount\":\"not-a-number\"}";
			producer.send(new ProducerRecord<>(TOPIC, "test-003", test3));
			Thread.sleep(5000);

			// Test Case 4: Completely invalid JSON
			System.out.println("\n📤 Test 4: Not JSON at all...");
			String test4 = "This is definitely not JSON!";
			producer.send(new ProducerRecord<>(TOPIC, "test-004", test4));
			Thread.sleep(5000);

			// Test Case 5: Empty message
			System.out.println("\n📤 Test 5: Empty message...");
			String test5 = "";
			producer.send(new ProducerRecord<>(TOPIC, "test-005", test5));
			Thread.sleep(5000);

			// Test Case 6: Null in unexpected place
			System.out.println("\n📤 Test 6: Null values...");
			String test6 = "{\"transactionID\":null,\"amount\":null}";
			producer.send(new ProducerRecord<>(TOPIC, "test-006", test6));
			Thread.sleep(5000);

			producer.flush();

			System.out.println("\n═══════════════════════════════════════════════════════════════");
			System.out.println("✅ All malformed messages sent!");
			System.out.println("═══════════════════════════════════════════════════════════════");
			System.out.println("📋 Expected behavior:");
			System.out.println("   1. Each message retries 3 times (watch for '🔄 Retry' logs)");
			System.out.println("   2. After retries exhausted → routes to transactions-dlt");
			System.out.println("   3. DLT consumer logs '⚠️ DEAD LETTER MESSAGE RECEIVED'");
			System.out.println("   4. Consumer continues processing (does NOT crash)");
			System.out.println("═══════════════════════════════════════════════════════════════");

		} catch (Exception e) {
			System.err.println("❌ Test failed: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
