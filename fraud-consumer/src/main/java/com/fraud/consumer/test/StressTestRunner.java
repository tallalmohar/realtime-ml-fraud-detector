package com.fraud.consumer.test;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sprint 6 - Task 5: Stress Test Runner
 * 
 * Sends 10,000+ transactions to test system performance under load.
 * 
 * Usage:
 * 1. Start all infrastructure (docker-compose up)
 * 2. Start fraud-consumer
 * 3. Run this class: mvn exec:java
 * -Dexec.mainClass="com.fraud.consumer.test.StressTestRunner"
 * 4. Monitor Grafana metrics during the test
 * 
 * Metrics to watch:
 * - Kafka consumer lag
 * - ML inference latency (p50, p95, p99)
 * - Database connection pool usage
 * - Memory usage
 * - Fraud detection rate
 */
public class StressTestRunner {

	private static final String KAFKA_BROKER = "localhost:9092";
	private static final String TOPIC = "transactions";
	private static final int TOTAL_TRANSACTIONS = 10000;
	private static final int BATCH_SIZE = 100;
	private static final long DELAY_BETWEEN_BATCHES_MS = 100; // 100ms between batches

	private static final Random random = new Random();
	private static final String[] PAYMENT_METHODS = { "CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "CRYPTO",
			"BANK_TRANSFER" };
	private static final String[] LOCATIONS = { "New York", "London", "Tokyo", "Paris", "Sydney", "Berlin", "Toronto" };
	private static final String[] MERCHANTS = { "Amazon", "Walmart", "Target", "BestBuy", "Costco", "Unknown Shop" };

	private static final AtomicLong sentCount = new AtomicLong(0);
	private static final AtomicLong errorCount = new AtomicLong(0);

	public static void main(String[] args) {
		System.out.println("═══════════════════════════════════════════════════════════════");
		System.out.println("🚀 STRESS TEST: Sending " + TOTAL_TRANSACTIONS + " transactions");
		System.out.println("═══════════════════════════════════════════════════════════════");
		System.out.println("Target: " + KAFKA_BROKER + " / " + TOPIC);
		System.out.println("Batch size: " + BATCH_SIZE);
		System.out.println("Delay between batches: " + DELAY_BETWEEN_BATCHES_MS + "ms");
		System.out.println("───────────────────────────────────────────────────────────────");

		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.ACKS_CONFIG, "1"); // Wait for leader only (faster)
		props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
		props.put(ProducerConfig.LINGER_MS_CONFIG, 5); // Small batching delay for throughput

		long startTime = System.currentTimeMillis();

		try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {

			for (int batch = 0; batch < TOTAL_TRANSACTIONS / BATCH_SIZE; batch++) {

				for (int i = 0; i < BATCH_SIZE; i++) {
					String transactionJson = generateTransaction();
					String key = UUID.randomUUID().toString();

					producer.send(new ProducerRecord<>(TOPIC, key, transactionJson), (metadata, exception) -> {
						if (exception != null) {
							errorCount.incrementAndGet();
							System.err.println("❌ Send failed: " + exception.getMessage());
						} else {
							sentCount.incrementAndGet();
						}
					});
				}

				// Progress update every batch
				long sent = sentCount.get();
				double progress = (sent * 100.0) / TOTAL_TRANSACTIONS;
				System.out.printf("📤 Progress: %d / %d (%.1f%%) - Errors: %d%n",
						sent, TOTAL_TRANSACTIONS, progress, errorCount.get());

				// Small delay between batches to prevent overwhelming
				if (DELAY_BETWEEN_BATCHES_MS > 0) {
					Thread.sleep(DELAY_BETWEEN_BATCHES_MS);
				}
			}

			producer.flush();

		} catch (Exception e) {
			System.err.println("❌ Stress test failed: " + e.getMessage());
			e.printStackTrace();
		}

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		double throughput = (sentCount.get() * 1000.0) / duration;

		System.out.println("───────────────────────────────────────────────────────────────");
		System.out.println("✅ STRESS TEST COMPLETE");
		System.out.println("───────────────────────────────────────────────────────────────");
		System.out.printf("📊 Total Sent: %d transactions%n", sentCount.get());
		System.out.printf("📊 Total Errors: %d%n", errorCount.get());
		System.out.printf("📊 Duration: %.2f seconds%n", duration / 1000.0);
		System.out.printf("📊 Throughput: %.2f transactions/second%n", throughput);
		System.out.println("───────────────────────────────────────────────────────────────");
		System.out.println("📈 Check Grafana for:");
		System.out.println("   - Consumer lag (should drop to 0 within 1-2 minutes)");
		System.out.println("   - ML inference latency (p99 < 50ms is good)");
		System.out.println("   - Fraud detection count");
		System.out.println("   - Database insert rate");
		System.out.println("═══════════════════════════════════════════════════════════════");
	}

	/**
	 * Generates a realistic transaction JSON with V1-V28 values.
	 */
	private static String generateTransaction() {
		String id = UUID.randomUUID().toString();
		String userId = "user-" + random.nextInt(1000);
		String merchantId = MERCHANTS[random.nextInt(MERCHANTS.length)];
		double amount = 10 + random.nextDouble() * 990; // $10 - $1000
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		String location = LOCATIONS[random.nextInt(LOCATIONS.length)];
		String paymentMethod = PAYMENT_METHODS[random.nextInt(PAYMENT_METHODS.length)];
		String transactionType = random.nextBoolean() ? "PURCHASE" : "WITHDRAWAL";

		// Generate V1-V28 with some variance (normal distribution centered at 0)
		StringBuilder vFields = new StringBuilder();
		for (int v = 1; v <= 28; v++) {
			double value = random.nextGaussian() * 2; // Mean 0, StdDev 2

			// Make some transactions suspicious (10% chance)
			if (random.nextDouble() < 0.10) {
				value = random.nextGaussian() * 5 + 3; // Higher values = more suspicious
			}

			vFields.append(String.format("\"v%d\":%.6f", v, value));
			if (v < 28)
				vFields.append(",");
		}

		double timeValue = random.nextDouble() * 86400; // Seconds in a day

		return String.format(
				"{\"transactionID\":\"%s\"," +
						"\"userId\":\"%s\"," +
						"\"merchantId\":\"%s\"," +
						"\"amount\":%.2f," +
						"\"timestamp\":\"%s\"," +
						"\"location\":\"%s\"," +
						"\"paymentMethod\":\"%s\"," +
						"\"transactionType\":\"%s\"," +
						"%s," +
						"\"time\":%.2f}",
				id, userId, merchantId, amount, timestamp, location,
				paymentMethod, transactionType, vFields.toString(), timeValue);
	}
}
