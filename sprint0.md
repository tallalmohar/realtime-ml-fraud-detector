# Sprint 0: Infrastructure & Project Skeleton - Detailed Breakdown

## Task 0.1: Initialize Multi-Module Maven/Gradle Project

### Sub-task 0.1.1: Choose Build Tool & Create Parent Project

**Why**: A multi-module structure allows you to separate concerns (producer, consumer, common models) while sharing dependencies and versions. Maven/Gradle parent projects enforce consistency.

**How**:

- Create a parent `pom.xml` (Maven) or root `build.gradle` (Gradle)
- Set Java version (17 or 21 recommended)
- Define Spring Boot version in `<parent>` or dependency management section
- Include: `spring-boot-starter-parent` as parent POM (Maven) or Spring Boot Gradle plugin

### Sub-task 0.1.2: Create Child Modules

**Why**: Separate modules prevent tight coupling. The `common` module will be a dependency for both producer and consumer.

**How**:

- Create 3 modules:
  - `fraud-common` (shared domain models)
  - `fraud-producer` (transaction generator)
  - `fraud-consumer` (ML inference engine)
- In parent POM: Use `<modules>` tag to declare them
- In Gradle: Use `settings.gradle` with `include` statements

### Sub-task 0.1.3: Configure Dependencies in Parent

**Why**: Centralized dependency management prevents version conflicts across modules.

**How**:

- Add `<dependencyManagement>` section (Maven) or `dependencies` block (Gradle)
- Key dependencies to declare versions for:
  - `spring-boot-starter-web`
  - `spring-kafka` (for producer/consumer)
  - `spring-boot-starter-data-jpa` (for database)
  - `postgresql` driver
  - `lombok` (optional, for cleaner POJOs)
  - `onnxruntime` (for Sprint 2)
  - `micrometer-registry-prometheus` (for Sprint 4)

---

## Task 0.2: Create `docker-compose.yml`

### Sub-task 0.2.1: Define Zookeeper Service

**Why**: Kafka requires Zookeeper for cluster coordination (pre-Kafka 3.x architecture, or if using KRaft mode, you can skip this).

**How**:

- Use image: `confluentinc/cp-zookeeper:latest`
- Expose port: `2181`
- Environment variables:
  - `ZOOKEEPER_CLIENT_PORT=2181`
  - `ZOOKEEPER_TICK_TIME=2000`

### Sub-task 0.2.2: Define Kafka Broker Service

**Why**: Kafka is the backbone of your event-driven architecture. This broker will host your `transactions` and `fraud-alerts` topics.

**How**:

- Use image: `confluentinc/cp-kafka:latest`
- Expose port: `9092` (external), `29092` (internal)
- Environment variables:
  - `KAFKA_BROKER_ID=1`
  - `KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181`
  - `KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092`
  - `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1`
- Add `depends_on: zookeeper`

### Sub-task 0.2.3: Define PostgreSQL Service

**Why**: Persistent storage for fraudulent transactions. Allows auditing and historical analysis.

**How**:

- Use image: `postgres:15-alpine`
- Expose port: `5432`
- Environment variables:
  - `POSTGRES_USER=fraud_user`
  - `POSTGRES_PASSWORD=fraud_pass`
  - `POSTGRES_DB=fraud_detection`
- Add volume for data persistence: `postgres_data:/var/lib/postgresql/data`

### Sub-task 0.2.4: Define Prometheus Service

**Why**: Metrics collection for monitoring fraud detection rate, latency, and system health.

**How**:

- Use image: `prom/prometheus:latest`
- Expose port: `9090`
- Mount a `prometheus.yml` config file (you'll create this in Sprint 4)
- Basic config: scrape your Spring Boot app at `localhost:8080/actuator/prometheus`

### Sub-task 0.2.5: Add Health Check & Networks

**Why**: Ensures services are ready before dependent services start. Networks isolate communication.

**How**:

- Add `healthcheck` to Kafka and PostgreSQL
- Define a custom network: `fraud-detection-network` with bridge driver
- Attach all services to this network

---

## Task 0.3: Define Shared Domain Model (Common Module)

### Sub-task 0.3.1: Create `Transaction` POJO

**Why**: This is the core data structure that flows through your entire pipeline. Standardizing it in a common module prevents duplication.

**How**:

- In `fraud-common/src/main/java/com/fraud/common/model/Transaction.java`
- Fields to include:
  - `String transactionId` (UUID)
  - `String userId`
  - `BigDecimal amount`
  - `String merchantId`
  - `LocalDateTime timestamp`
  - `String location` (optional: latitude/longitude or city)
  - `String paymentMethod` (e.g., CREDIT_CARD, DEBIT_CARD)
- Annotations:
  - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` (if using Lombok)
  - Or manually write getters/setters/constructors

### Sub-task 0.3.2: Add JSON Serialization Support

**Why**: Kafka messages will be JSON-formatted. You need to serialize/deserialize Transaction objects.

**How**:

- Add `com.fasterxml.jackson.core:jackson-databind` dependency
- Add `@JsonProperty` annotations if field names differ from JSON keys
- Optional: Create a custom `TransactionSerializer` and `TransactionDeserializer` implementing Kafka's `Serializer<Transaction>` and `Deserializer<Transaction>`

### Sub-task 0.3.3: Make Common Module Available

**Why**: Both producer and consumer modules need to depend on this shared model.

**How**:

- In `fraud-producer` and `fraud-consumer` POMs:
  - Add dependency: `<dependency><groupId>com.fraud</groupId><artifactId>fraud-common</artifactId></dependency>`
- Ensure version is inherited from parent

---

## Task 0.4: Test Infrastructure Connectivity

### Sub-task 0.4.1: Start Docker Services

**Why**: Verify that all containers start successfully without errors.

**How**:

- Run: `docker-compose up -d`
- Check logs: `docker-compose logs -f`
- Verify all 4 services are running: `docker ps`

### Sub-task 0.4.2: Test Kafka Connectivity

**Why**: Ensure Kafka broker is accepting connections and can create topics.

**How**:

- Use Kafka CLI tools (from inside container or installed locally):
  - Create topic: `kafka-topics --create --topic transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1`
  - List topics: `kafka-topics --list --bootstrap-server localhost:9092`
- Alternative: Write a simple Java test using `AdminClient` from `spring-kafka`

### Sub-task 0.4.3: Test PostgreSQL Connectivity

**Why**: Confirm database is accessible and accepting queries.

**How**:

- Connect via CLI: `docker exec -it <postgres-container-id> psql -U fraud_user -d fraud_detection`
- Or use a DB client (DBeaver, pgAdmin)
- Run a test query: `SELECT version();`
- Create a test table to verify write permissions

### Sub-task 0.4.4: Test Prometheus Scraping (Basic Setup)

**Why**: Ensure Prometheus can reach the actuator endpoint (you'll fully configure this in Sprint 4).

**How**:

- Navigate to: `http://localhost:9090`
- Check Targets page: `http://localhost:9090/targets`
- Even if Spring Boot app isn't running yet, verify Prometheus UI loads
- Create a placeholder `prometheus.yml` with:
  ```yaml
  scrape_configs:
    - job_name: "spring-boot-fraud-detection"
      metrics_path: "/actuator/prometheus"
      static_configs:
        - targets: ["host.docker.internal:8080"]
  ```

---

## Sprint 0 Completion Checklist

Once all sub-tasks are done, you should have:

- ✅ A multi-module project structure (3 modules: common, producer, consumer)
- ✅ Docker Compose running Kafka, Zookeeper, PostgreSQL, Prometheus
- ✅ A `Transaction` POJO in the common module
- ✅ Verified connectivity to Kafka (can create topics) and PostgreSQL (can connect)

---

**Ready to implement?** Start with **Task 0.1** (project structure), and once you've created the parent POM and modules, share your setup for a pair programming review! 🚀
