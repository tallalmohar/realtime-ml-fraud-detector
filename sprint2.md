# Sprint 2: Core Analysis Consumer & ML Integration

## Overview

In Sprint 2, we'll build the **fraud-consumer** service that listens to the Kafka "transactions" topic, processes each transaction through a machine learning model, and determines if it's fraudulent. This is the heart of your real-time fraud detection system.

**Technologies:**

- Spring Boot + Spring Kafka (Consumer)
- ONNX Runtime (ML model inference in Java)
- Feature Engineering (data transformation)
- Machine Learning model integration

---

## Task 1: Set Up Spring Boot Consumer Application

### 1.1: Add Spring Boot Dependencies to fraud-consumer

**Why:** The fraud-consumer needs Spring Boot capabilities to run as a service and consume from Kafka.

**Dependencies to add to `fraud-consumer/pom.xml`:**

- `spring-boot-starter` - Core Spring Boot
- `spring-boot-starter-web` - REST capabilities (for health checks later)
- `spring-kafka` - Kafka consumer integration

**How it fits:** This sets up the consumer service foundation, mirroring the producer structure.

### 1.2: Add ONNX Runtime Dependency

**Why:** ONNX Runtime allows Java applications to load and run machine learning models that were trained in Python (scikit-learn, TensorFlow, PyTorch, etc.). It's the bridge between Python ML and Java production systems.

**Dependency to add:**

- `onnxruntime` (version 1.17.1 already managed in parent pom.xml)

**How it fits:** This is the ML inference engine that will score each transaction in real-time.

### 1.3: Create FraudConsumerApplication Main Class

**Why:** Every Spring Boot application needs an entry point with `@SpringBootApplication`.

**Create:** `fraud-consumer/src/main/java/com/fraud/consumer/FraudConsumerApplication.java`

- Add `@SpringBootApplication` annotation
- Add `main` method with `SpringApplication.run()`

**How it fits:** This starts the consumer application and activates all Spring components.

### 1.4: Create application.properties for Consumer

**Why:** Configure Kafka consumer settings and Spring Boot properties.

**Create:** `fraud-consumer/src/main/resources/application.properties`

**Key configurations needed:**

- `spring.kafka.bootstrap-servers=localhost:9092`
- `spring.kafka.consumer.group-id=fraud-detection-group` (Consumer group for load balancing)
- `spring.kafka.consumer.auto-offset-reset=earliest` (Start from beginning if no offset)
- `spring.kafka.consumer.key-deserializer=StringDeserializer`
- `spring.kafka.consumer.value-deserializer=JsonDeserializer`
- `spring.kafka.consumer.properties.spring.json.trusted.packages=*` (Trust Transaction class)
- `server.port=8081` (Different port from producer)

**How it fits:** These settings tell the consumer how to connect to Kafka and deserialize the Transaction JSON.

---

## Task 2: Implement Kafka Listener

### 2.1: Create TransactionConsumer Service

**Why:** This is the entry point where messages arrive from Kafka. The `@KafkaListener` annotation automatically polls the topic and hands you deserialized Transaction objects.

**Create:** `fraud-consumer/src/main/java/com/fraud/consumer/service/TransactionConsumer.java`

- Annotate with `@Service`
- Create method with `@KafkaListener(topics = "transactions", groupId = "fraud-detection-group")`
- Method should accept `Transaction` object as parameter
- Log the received transaction for now

**How it fits:** This is the ingestion layer. Every transaction from the producer flows through this method.

### 2.2: Test the Consumer Setup

**Why:** Before adding ML complexity, verify that messages are being consumed correctly.

**Test:**

- Run fraud-producer (already running)
- Run fraud-consumer from IntelliJ
- Check logs to see if transactions are being consumed

**How it fits:** Validates the Kafka consumer configuration is correct before proceeding.

---

## Task 3: ML Model Configuration

### 3.1: Create Placeholder ONNX Model File

**Why:** We need a model file to load. In Sprint 5 we'll train a real model, but for now we'll create a simple placeholder to test the infrastructure.

**Create:** `fraud-consumer/src/main/resources/fraud_model.onnx`

- For now, create an empty file or use a simple dummy model
- Document that this will be replaced in Sprint 5

**How it fits:** Establishes the file location and loading pattern before the real model exists.

### 3.2: Create OnnxModelConfig Bean

**Why:** Loading an ONNX model is resource-intensive (can be 50-500 MB). We want to load it ONCE at application startup and share it across all requests. Spring's `@Bean` with singleton scope does exactly this.

**Create:** `fraud-consumer/src/main/java/com/fraud/consumer/config/OnnxModelConfig.java`

- Annotate with `@Configuration`
- Create `@Bean` method that returns `OrtSession` (ONNX Runtime session)
- Load the model from `classpath:fraud_model.onnx`
- Add error handling for model loading failures

**Key ONNX concepts:**

- `OrtEnvironment` - ONNX runtime environment (global)
- `OrtSession` - Loaded model ready for inference (reusable, thread-safe)

**How it fits:** This bean is injected into services that need to perform inference, ensuring only one model copy in memory.

### 3.3: Document Model Input Requirements

**Why:** ML models expect data in a specific format: exact feature order, data types, and shapes.

**Create:** Comment documentation in the config class specifying:

- Expected input tensor shape (e.g., [1, 10] for 1 transaction with 10 features)
- Feature order (e.g., amount, hour, merchant_hash, etc.)
- Data types (float32, int64, etc.)

**How it fits:** This is the contract between your feature engineering and the model. Gets populated in Sprint 5 when we train the real model.

---

## Task 4: Feature Engineering Service

### 4.1: Create FeatureEngineeringService

**Why:** Raw Transaction objects can't be fed directly into ML models. Models need numerical features in a specific format. Feature engineering transforms:

- `LocalDateTime timestamp` → `hour_of_day`, `day_of_week`, `is_weekend`
- `String location` → `location_hash` or category
- `String paymentMethod` → one-hot encoding
- `BigDecimal amount` → normalized float

**Create:** `fraud-consumer/src/main/java/com/fraud/consumer/service/FeatureEngineeringService.java`

- Annotate with `@Service`
- Create method `float[] extractFeatures(Transaction transaction)`
- Implement feature transformations

**Example features to extract:**

1. **amount** (normalized: divide by max expected amount like 10,000)
2. **hour_of_day** (0-23 extracted from timestamp)
3. **day_of_week** (1-7 from timestamp)
4. **is_weekend** (0 or 1)
5. **payment_method_encoded** (CREDIT_CARD=0, DEBIT_CARD=1, etc.)
6. **merchant_id_hash** (hash of merchant string % 1000)
7. **location_hash** (hash of location string % 1000)

**How it fits:** This service sits between the Kafka listener and the ML model, preparing data for inference.

### 4.2: Add Feature Normalization Logic

**Why:** ML models train on data within specific ranges (often 0-1). If your training data had amounts from $1-$5000, but production sees $50,000, the model will fail. Normalization ensures consistency.

**Implement in FeatureEngineeringService:**

- Min-Max scaling for `amount`: `(amount - min) / (max - min)`
- Standard scaling if you know mean/std from training data

**How it fits:** Ensures the model receives data in the same distribution it was trained on.

### 4.3: Handle Missing or Invalid Data

**Why:** Real-world data is messy. What if `timestamp` is null? Or `paymentMethod` is "UNKNOWN"?

**Implement:**

- Default values for missing fields
- Validation before feature extraction
- Logging for suspicious data

**How it fits:** Makes the system robust to data quality issues.

---

## Task 5: ML Inference Service

### 5.1: Create FraudDetectionService

**Why:** This service orchestrates the fraud detection flow: receive transaction → extract features → run inference → interpret results.

**Create:** `fraud-consumer/src/main/java/com/fraud/consumer/service/FraudDetectionService.java`

- Annotate with `@Service`
- Inject `OrtSession` (from OnnxModelConfig)
- Inject `FeatureEngineeringService`
- Create method `boolean isFraudulent(Transaction transaction)`

**How it fits:** This is the core business logic that combines feature engineering and ML inference.

### 5.2: Implement ONNX Inference Logic

**Why:** This is where the ML model actually runs and predicts fraud probability.

**Implement in FraudDetectionService:**

```java
public boolean isFraudulent(Transaction transaction) throws OrtException {
    // 1. Extract features
    float[] features = featureEngineeringService.extractFeatures(transaction);

    // 2. Create ONNX tensor from features
    OnnxTensor inputTensor = OnnxTensor.createTensor(
        ortEnvironment,
        new float[][]{features}  // Shape: [1, num_features]
    );

    // 3. Run inference
    Map<String, OnnxTensor> inputs = Map.of("input", inputTensor);
    OrtSession.Result result = ortSession.run(inputs);

    // 4. Extract prediction (probability of fraud)
    float[][] output = (float[][]) result.get(0).getValue();
    float fraudProbability = output[0][1];  // Assuming [prob_clean, prob_fraud]

    // 5. Threshold decision
    return fraudProbability > 0.5;  // Adjust threshold as needed
}
```

**Key ONNX concepts:**

- `OnnxTensor` - Wrapper for input/output data
- `OrtSession.run()` - Executes the model
- Output interpretation depends on your model type (classification, regression)

**How it fits:** This is the actual fraud detection happening in real-time.

### 5.3: Integrate Inference into Kafka Listener

**Why:** Now we connect all the pieces: consume → detect → log results.

**Update TransactionConsumer:**

```java
@KafkaListener(topics = "transactions", groupId = "fraud-detection-group")
public void consumeTransaction(Transaction transaction) {
    try {
        boolean isFraud = fraudDetectionService.isFraudulent(transaction);

        if (isFraud) {
            log.warn("🚨 FRAUD DETECTED: {}", transaction.getTransactionID());
            // TODO Sprint 3: Save to database and send alert
        } else {
            log.info("✅ Transaction OK: {}", transaction.getTransactionID());
        }
    } catch (Exception e) {
        log.error("Error processing transaction: {}", transaction.getTransactionID(), e);
    }
}
```

**How it fits:** This completes the real-time fraud detection pipeline: Kafka → Feature Engineering → ML Model → Decision.

---

## Task 6: Testing & Validation

### 6.1: Create Simple Test Model (Temporary)

**Why:** We don't have a trained model yet (that's Sprint 5), but we can create a dummy rule-based detector to test the flow.

**Temporary implementation in FraudDetectionService:**

- Instead of ONNX inference, use simple rules:
  - If amount > $900 → flag as fraud
  - If paymentMethod == "CRYPTO" → flag as fraud
  - Otherwise → legitimate

**How it fits:** Allows end-to-end testing before the real model exists.

### 6.2: Run End-to-End Test

**Why:** Verify the entire pipeline works: Producer → Kafka → Consumer → Feature Engineering → Detection → Logging.

**Test steps:**

1. Start Docker containers (already running)
2. Run fraud-producer (port 8080)
3. Run fraud-consumer (port 8081)
4. Watch consumer logs for fraud detections
5. Verify ~10% of transactions are flagged (based on dummy rules)

**How it fits:** Proves the architecture works before adding complexity.

### 6.3: Add Basic Metrics Logging

**Why:** Track system performance and detection rates.

**Add to FraudDetectionService:**

- Count total transactions processed
- Count fraud detections
- Log every 100 transactions: "Processed 100 tx, detected 12 fraud (12%)"

**How it fits:** Provides visibility into system operation and fraud patterns.

---

## Sprint 2 Success Criteria ✅

- [ ] fraud-consumer Spring Boot application runs on port 8081
- [ ] Consumer successfully receives transactions from Kafka
- [ ] Feature engineering extracts 7+ numerical features from each transaction
- [ ] ONNX model loading infrastructure is in place (even with dummy model)
- [ ] Fraud detection service classifies transactions (even with simple rules)
- [ ] Console logs show both legitimate and fraud classifications
- [ ] No consumer crashes or uncaught exceptions
- [ ] System can process 100+ transactions without errors

---

## Notes

**After Sprint 2 Completion:**

- You'll have a working consumer that processes transactions in real-time
- The ML model will be a placeholder (rule-based or dummy)
- In Sprint 3, we'll add database persistence and alerts
- In Sprint 5, we'll replace the dummy model with a real trained ML model

**Key Learning Concepts:**

- **Kafka Consumer Groups:** Multiple consumers can share the load
- **ONNX Runtime:** Run Python-trained models in Java production systems
- **Feature Engineering:** Transform raw data into ML-ready format
- **Real-time Inference:** Process streaming data with ML models
- **Microservices:** Producer and Consumer are independent services

**Common Pitfalls:**

- Model input shape mismatch (e.g., expecting 10 features but sending 7)
- Deserialization errors (make sure Transaction class is in fraud-common)
- Memory issues (don't create new OrtSession per request, use singleton)
- Feature order mismatch (model trained on [A,B,C] but you send [B,A,C])
