# Project: Real-Time Financial Fraud Detection System

## Sprint 0: Infrastructure & Project Skeleton
- [X] Initialize Multi-Module Maven/Gradle Project
- [X] Create `docker-compose.yml` (Zookeeper, Kafka, PostgreSQL, Prometheus)
- [X] Define Shared Domain Model (Common module for Transaction POJO)
- [X] Test Infrastructure connectivity (Verify Kafka and DB are reachable)

## Sprint 1: The Transaction Producer (The Stream)
- [X] Add `JavaFaker` dependency for mock data generation
- [X] Implement `Transaction` serialization logic (JSON)
- [X] Configure `KafkaTemplate` with Producer properties
- [X] Create `TransactionGeneratorService` with `@Scheduled` task
- [X] Validate data flow: Verify messages appear in Kafka 'transactions' topic

## Sprint 2: Core Analysis Consumer & ML Integration
- [X] Add `onnxruntime` dependency to Spring Boot project
- [X] Set up `@KafkaListener` to ingest transaction stream
- [X] Implement `OnnxModelConfig` to load `.onnx` file as a Singleton Bean
- [X] Create `FeatureEngineeringService` (Normalize/Scale input data for the model)
- [X] Implement Inference Logic (Convert Transaction -> Tensor -> Prediction)

## Sprint 3: The Data Sink & Alerting
- [X] Configure Spring Data JPA and PostgreSQL connection
- [X] Create `FraudulentTransaction` Entity and Repository
- [X] Implement logic to route "Clean" vs "Fraud" transactions
- [X] Set up a secondary Kafka Producer for the 'fraud-alerts' topic
- [X] Verify persistence: Check PostgreSQL for flagged records

## Sprint 4: Monitoring & Observability
- [X] Add `Micrometer` and `Actuator` dependencies
- [X] Define custom `Counter` for "Total Transactions" and "Fraud Detected"
- [X] Define `Timer` or `Gauge` for Inference Latency (ms)
- [X] Configure Prometheus scrape target in `application.yml`
- [X] (Optional) Create a basic Grafana dashboard to visualize fraud spikes

## Sprint 5: The ML Pipeline Bridge (Python to Java)
- [X] Prepare Python environment (Scikit-Learn, Pandas, Onnx-tools)
- [X] Train `RandomForest` or `XGBoost` on Kaggle Credit Card dataset
- [X] Export the trained model to `fraud_model.onnx`
- [X] Document the feature order (The exact sequence of columns the model expects)
- [X] Move `.onnx` file to Java `src/main/resources`

## Sprint 6: Resilience & Testing
- [ ] Implement a **Dead Letter Topic (DLT)** for malformed JSON messages
- [ ] Add `Retryable` logic for transient Database/Kafka failures
- [ ] Write Integration Test using `@EmbeddedKafka`
- [ ] Write Unit Test for Feature Engineering logic
- [ ] Final "Stress Test": Pump 10,000 transactions and verify system stability