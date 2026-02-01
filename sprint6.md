# Sprint 6: Resilience & Testing 🛡️

**Goal**: Make the fraud detection system production-ready with comprehensive error handling, retry logic, testing, and resilience patterns.

---

## 📋 Sprint Tasks

### Task 1: Dead Letter Topic (DLT) Setup

**Problem**: Malformed JSON or unparseable messages currently crash the consumer  
**Solution**: Route failed messages to a separate Kafka topic for investigation

- [x] Create `transactions-dlt` Kafka topic
- [x] Configure Spring Kafka error handler
- [x] Add `@KafkaListener` for DLT topic (logging/monitoring)
- [x] Test with intentionally malformed JSON

**Why**: In production, bad data happens (network corruption, producer bugs, schema changes). We need to isolate failures without stopping the entire pipeline.

---

### Task 2: Retry Logic for Transient Failures

**Problem**: Database/Kafka temporary issues cause permanent failures  
**Solution**: Implement exponential backoff retry with circuit breaker

- [x] Add `@Retryable` to database operations
- [x] Configure retry policy (3 attempts, exponential backoff)
- [x] Add `@Recover` fallback methods
- [ ] Test retry behavior with Docker container restarts

**Why**: Microservices must handle transient failures gracefully. A 1-second DB hiccup shouldn't lose fraud alerts.

---

### Task 3: Integration Tests with Embedded Kafka

**Problem**: No automated tests for end-to-end message flow  
**Solution**: Use `@EmbeddedKafka` to test producer → consumer → database flow

- [x] Add `spring-kafka-test` dependency
- [ ] Create `FraudDetectionIntegrationTest` class
- [ ] Test: Send transaction → Verify fraud detection → Check DB persistence
- [ ] Test: DLT routing for malformed messages
- [ ] Test: Retry logic with transient failures

**Why**: Manual testing doesn't scale. Integration tests catch regressions early and document expected behavior.

---

### Task 4: Unit Tests for ML Pipeline

**Problem**: No tests for critical feature engineering and ONNX inference  
**Solution**: Isolated unit tests for each component

- [x] Test `FeatureEngineeringService.extractFeatures()`
  - Verify normalization (amount, time)
  - Verify V1-V28 extraction
  - Verify 30-element array output
- [x] Test `FraudDetectionService.detectFraudUsingModel()`
  - Mock ONNX session
  - Verify probability extraction
  - Verify threshold logic (>50% = fraud)
- [x] Test edge cases:
  - Null values
  - Missing V fields
  - ONNX inference failures

**Why**: ML code is complex and brittle. Unit tests ensure feature engineering stays consistent when code changes.

---

### Task 5: Stress Testing

**Problem**: Unknown system behavior under high load  
**Solution**: Load test with 10,000+ transactions, measure performance

- [x] Create stress test script (producer sends 10K transactions)
- [ ] Monitor metrics:
  - Kafka consumer lag
  - Database connection pool usage
  - ML inference latency (p50, p95, p99)
  - Memory usage (heap, ONNX model)
  - CPU usage
- [ ] Identify bottlenecks:
  - Is ML inference the bottleneck? (~10ms/transaction)
  - Is DB the bottleneck? (Batch inserts?)
  - Is Kafka the bottleneck? (Partition count?)
- [ ] Document findings and optimization recommendations

**Why**: Production systems must handle peak loads (Black Friday, market events). Stress tests reveal scalability limits before customers do.

---

## 🎯 Success Criteria

✅ **Resilience**:

- System survives malformed messages without crashing
- Database failures trigger retries, not data loss
- Circuit breaker prevents cascade failures

✅ **Testing**:

- 80%+ code coverage on core logic
- Integration tests pass consistently
- Unit tests run in <5 seconds

✅ **Performance**:

- Handles 1000+ transactions/second without lag
- ML inference latency <50ms p99
- Database connection pool never exhausted
- Memory stable under continuous load

---

## 🔧 Technical Components to Build

### 1. **ErrorHandlingConfig.java**

```java
@Configuration
public class ErrorHandlingConfig {
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // Route to DLT after 3 failed attempts
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
```

### 2. **RetryConfig.java**

```java
@Configuration
@EnableRetry
public class RetryConfig {
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(100);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000);
        template.setBackOffPolicy(backOff);
        return template;
    }
}
```

### 3. **FraudDetectionIntegrationTest.java**

```java
@SpringBootTest
@EmbeddedKafka(topics = {"transactions", "fraud-alerts", "transactions-dlt"})
public class FraudDetectionIntegrationTest {
    @Test
    void testFraudDetectionFlow() {
        // Send transaction → Verify fraud detected → Check DB
    }
}
```

### 4. **StressTestRunner.java**

```java
public class StressTestRunner {
    public static void main(String[] args) {
        // Send 10,000 transactions with realistic V1-V28
        // Measure latency, throughput, error rate
    }
}
```

---

## 📊 Monitoring Enhancements

Add new Prometheus metrics:

```java
// Error tracking
Counter kafkaDeserializationErrors;
Counter databaseRetryAttempts;
Counter onnxInferenceErrors;

// Performance
Timer featureExtractionLatency;
Gauge kafkaConsumerLag;
Gauge databaseConnectionPoolActive;
```

---

## 🚀 Sprint Workflow

1. **Day 1**: DLT setup + Error handling config
2. **Day 2**: Retry logic + Circuit breaker
3. **Day 3**: Integration tests (Kafka flow)
4. **Day 4**: Unit tests (ML pipeline)
5. **Day 5**: Stress testing + Performance tuning

---

## 📝 Deliverables

- [ ] DLT topic configured and tested
- [ ] Retry logic with exponential backoff
- [ ] 10+ integration tests passing
- [ ] 20+ unit tests passing
- [ ] Stress test report with performance metrics
- [ ] Updated README with testing instructions
- [ ] Circuit breaker configuration documented

---

## 🎓 Key Learnings

**Resilience Patterns**:

- Dead Letter Topics prevent message loss
- Retries handle transient failures
- Circuit breakers prevent cascade failures
- Idempotency keys prevent duplicate processing

**Testing Pyramid**:

- Many unit tests (fast, isolated)
- Some integration tests (realistic, slower)
- Few E2E tests (expensive, brittle)

**Performance Optimization**:

- Batch database inserts (10x faster)
- Connection pooling (avoid exhaustion)
- ONNX model singleton (load once)
- Async processing (don't block Kafka consumer)

---

Let's make this system **bulletproof**! 🛡️
