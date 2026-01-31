# Sprint 4: Monitoring & Observability

## Overview

In Sprint 4, we'll add **metrics collection and monitoring** to your fraud detection system. Right now, your system works perfectly - it detects fraud, saves to database, and sends alerts - but you have **no visibility into performance**. You can't answer critical questions like:

- How many transactions per second are we processing?
- What percentage are fraudulent?
- Is the system slowing down under load?
- Are there any bottlenecks?

**Technologies:**

- Micrometer (metrics collection library)
- Spring Boot Actuator (production-ready features)
- Prometheus (metrics storage & querying)
- Grafana (visualization - optional)

---

## Why Monitoring Matters

**Production systems need observability:**

- **Performance tracking**: Detect slowdowns before users complain
- **Capacity planning**: Know when to scale up
- **Fraud analysis**: Track fraud rates over time
- **SLA monitoring**: Ensure system meets requirements (e.g., process transactions in <100ms)
- **Debugging**: Quickly identify issues during incidents

**Without metrics, you're flying blind!** 🎯

---

## Task 1: Add Dependencies

### 1.1: Add Micrometer & Actuator to pom.xml

**Why:** These libraries expose metrics in a format Prometheus can scrape.

**What to add to `fraud-consumer/pom.xml`:**

```xml
<!-- Metrics & Monitoring -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**How it fits:**

- **Micrometer**: Vendor-neutral metrics facade (works with Prometheus, Datadog, etc.)
- **Actuator**: Exposes `/actuator/prometheus` endpoint for Prometheus to scrape
- Spring Boot auto-configures everything - just add dependencies!

**After adding, run:** `mvn clean install` from project root

---

## Task 2: Configure Actuator Endpoints

### 2.1: Enable Prometheus Endpoint in application.properties

**Why:** By default, Actuator exposes limited endpoints. You need to enable Prometheus specifically.

**Add to `fraud-consumer/src/main/resources/application.properties`:**

```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true
```

**What each property does:**

- `exposure.include`: Which endpoints are accessible via HTTP
- `prometheus.enabled`: Turns on `/actuator/prometheus` endpoint
- `metrics.export.prometheus.enabled`: Enables Prometheus format export

**Test it:** After restarting consumer, visit:

```
http://localhost:8081/actuator/prometheus
```

You should see metrics like:

```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Eden Space",} 2.5165824E7
```

**How it fits:** This endpoint is what Prometheus scrapes every 15 seconds to collect metrics.

---

## Task 3: Create Custom Metrics

### 3.1: Create MetricsService to Track Business Metrics

**Why:** Spring Boot provides JVM metrics (memory, threads) automatically, but you need **custom business metrics** like:

- How many transactions processed?
- How many frauds detected?
- How long does fraud detection take?

**Create:** `fraud-consumer/src/main/java/com/fraud/consumer/service/MetricsService.java`

**What to implement:**

```java
@Service
public class MetricsService {
    private final Counter totalTransactionsCounter;
    private final Counter fraudDetectedCounter;
    private final Counter cleanTransactionsCounter;
    private final Timer fraudDetectionTimer;
    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Counter: Total transactions processed
        this.totalTransactionsCounter = Counter.builder(fraud.transactions.total")
                .description("Total number of transactions processed")
                .register(meterRegistry);

        // Counter: Fraud detected
        this.fraudDetectedCounter = Counter.builder("fraud.transactions.fraudulent")
                .description("Total number of fraudulent transactions detected")
                .register(meterRegistry);

        // Counter: Clean transactions
        this.cleanTransactionsCounter = Counter.builder("fraud.transactions.clean")
                .description("Total number of clean transactions")
                .register(meterRegistry);

        // Timer: Detection latency
        this.fraudDetectionTimer = Timer.builder("fraud.detection.latency")
                .description("Time taken to detect fraud")
                .publishPercentiles(0.5, 0.95, 0.99) // Median, 95th, 99th percentile
                .register(meterRegistry);
    }

    public void recordTransaction() {
        totalTransactionsCounter.increment();
    }

    public void recordFraud() {
        fraudDetectedCounter.increment();
    }

    public void recordClean() {
        cleanTransactionsCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDetectionTime(Timer.Sample sample) {
        sample.stop(fraudDetectionTimer);
    }
}
```

**Key concepts:**

**Counter**: A monotonically increasing value (never decreases)

- Use for: Total transactions, fraud count, errors
- Example: `fraud.transactions.total` increases by 1 for each transaction

**Timer**: Measures duration and rate

- Use for: Operation latency, request duration
- Automatically tracks: count, total time, max time, percentiles
- Example: `fraud.detection.latency` tracks how long `isFraudulent()` takes

**Gauge**: A value that can go up or down

- Use for: Queue size, active connections, memory usage
- Example: Number of transactions in processing queue

**How it fits:** This service is injected into TransactionConsumer to track all fraud detection activity.

---

### 3.2: Integrate Metrics into TransactionConsumer

**Why:** You need to call MetricsService at key points in your fraud detection flow.

**Modify:** `fraud-consumer/src/main/java/com/fraud/consumer/service/TransactionConsumer.java`

**Changes needed:**

1. **Inject MetricsService** in constructor
2. **Start timer** before fraud detection
3. **Record total transactions** when message received
4. **Record fraud/clean** after detection
5. **Stop timer** after detection completes

**Example integration points:**

```java
@KafkaListener(topics = "transactions", groupId = "fraud-detection-group")
public void consumeTransaction(Transaction transaction) {
    log.info("Received transaction: ID={}", transaction.getTransactionID());

    metricsService.recordTransaction(); // ← Count total
    Timer.Sample sample = metricsService.startTimer(); // ← Start timing

    try {
        FraudResult result = fraudDetectionService.isFraudulent(transaction);

        metricsService.recordDetectionTime(sample); // ← Stop timing

        if(result.isFraud()){
            metricsService.recordFraud(); // ← Count fraud
            // ... save and alert
        } else {
            metricsService.recordClean(); // ← Count clean
            log.info("Transaction OK: {}", transaction.getTransactionID());
        }
    } catch(Exception e) {
        metricsService.recordDetectionTime(sample); // ← Stop timing even on error
        log.error("Error processing transaction", e);
    }
}
```

**How it fits:** Now every transaction flows through metrics tracking, giving you complete visibility.

---

## Task 4: Configure Prometheus Scraping

### 4.1: Update prometheus.yml Configuration

**Why:** Prometheus needs to know WHERE to scrape metrics from (your consumer's actuator endpoint).

**Your existing `prometheus.yml` should have:**

```yaml
scrape_configs:
  - job_name: "fraud-consumer"
    metrics_path: "/actuator/prometheus"
    scrape_interval: 15s
    static_configs:
      - targets: ["host.docker.internal:8081"]
```

**Key configuration:**

- `job_name`: Label to identify this service in Prometheus
- `metrics_path`: Where actuator exposes metrics
- `scrape_interval`: How often to collect (15s = every 15 seconds)
- `targets`: Consumer's address (host.docker.internal works from Docker to host machine)

**Verify your prometheus.yml has this configuration.** If not, add it!

**How it fits:** Prometheus runs in Docker, periodically calls `http://host.docker.internal:8081/actuator/prometheus`, and stores the metrics in its time-series database.

---

### 4.2: Restart Prometheus Container

**Why:** Prometheus reads config file only at startup.

**Run:**

```bash
docker-compose restart prometheus
```

**Verify scraping works:**

1. Open Prometheus UI: http://localhost:9090
2. Go to Status → Targets
3. Check `fraud-consumer` endpoint shows **UP** (green)

If it shows **DOWN**, check:

- Consumer is running on port 8081
- `/actuator/prometheus` endpoint is accessible
- Docker can reach host machine via `host.docker.internal`

**How it fits:** Prometheus is now actively collecting your custom metrics every 15 seconds.

---

## Task 5: Query Metrics in Prometheus

### 5.1: Test Your Metrics Using PromQL

**Why:** Verify metrics are being collected and learn how to query them.

**Open Prometheus:** http://localhost:9090

**Example queries to try:**

**1. Total transactions processed:**

```promql
fraud_transactions_total
```

**2. Fraud rate (percentage):**

```promql
(fraud_transactions_fraudulent / fraud_transactions_total) * 100
```

**3. Transactions per second:**

```promql
rate(fraud_transactions_total[1m])
```

**4. Fraud detection latency (99th percentile):**

```promql
fraud_detection_latency{quantile="0.99"}
```

**5. Average detection time over last 5 minutes:**

```promql
rate(fraud_detection_latency_sum[5m]) / rate(fraud_detection_latency_count[5m])
```

**How it fits:** PromQL (Prometheus Query Language) lets you analyze metrics, create dashboards, and set up alerts.

---

## Task 6: (Optional) Create Grafana Dashboard

### 6.1: Set Up Grafana for Visualization

**Why:** Prometheus UI is basic. Grafana provides beautiful, customizable dashboards.

**Add to `docker-compose.yml`:**

```yaml
grafana:
  image: grafana/grafana:latest
  container_name: grafana
  networks:
    - fraud-detection-network
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
  volumes:
    - grafana_data:/var/lib/grafana
```

**Add to volumes section:**

```yaml
volumes:
  postgres_data:
  grafana_data: # Add this
```

**Start Grafana:**

```bash
docker-compose up -d grafana
```

**Access Grafana:**

1. Open: http://localhost:3000
2. Login: admin / admin
3. Add Data Source: Prometheus (http://prometheus:9090)
4. Create Dashboard with panels for:
   - Total transactions (graph over time)
   - Fraud rate (gauge)
   - Detection latency (heatmap)
   - Transactions per second (graph)

**How it fits:** Grafana queries Prometheus and displays real-time dashboards. Perfect for NOC screens, incident response, and demos!

---

## Testing & Verification

### Verify Sprint 4 Success

**1. Check actuator endpoint:**

```bash
curl http://localhost:8081/actuator/prometheus | grep fraud
```

Should show your custom metrics:

```
fraud_transactions_total 1500.0
fraud_transactions_fraudulent 45.0
fraud_detection_latency_sum 125.5
```

**2. Query Prometheus:**

- Go to http://localhost:9090/graph
- Enter: `fraud_transactions_total`
- Click Execute
- See increasing counter

**3. Verify fraud rate calculation:**

- Let system run for 1-2 minutes
- Query: `(fraud_transactions_fraudulent / fraud_transactions_total) * 100`
- Should show ~3-5% fraud rate (based on your rules)

**4. Check latency:**

- Query: `fraud_detection_latency{quantile="0.99"}`
- Should show sub-millisecond times (rule-based is fast!)
- In Sprint 5 (ML model), this will increase

---

## Key Metrics to Monitor

**Operational Metrics:**

- `fraud_transactions_total`: Overall throughput
- `rate(fraud_transactions_total[1m])`: Transactions/second

**Business Metrics:**

- `fraud_transactions_fraudulent`: Total fraud count
- `(fraud_transactions_fraudulent / fraud_transactions_total) * 100`: Fraud percentage

**Performance Metrics:**

- `fraud_detection_latency`: Detection time (p50, p95, p99)
- `jvm_memory_used_bytes`: Memory usage
- `jvm_threads_live`: Thread count

**Alert Conditions (for future):**

- Fraud rate > 10% → Potential attack
- Latency p99 > 100ms → Performance degradation
- Error rate > 1% → System issue

---

## Architecture Impact

**Before Sprint 4:**

```
Producer → Kafka → Consumer → DB
                          ↓
                      Alerts
```

**After Sprint 4:**

```
Producer → Kafka → Consumer → DB
                     ↓     ↓
                  Alerts  Metrics
                            ↓
                       Prometheus
                            ↓
                        Grafana
```

**What you gain:**

- ✅ Real-time visibility into system health
- ✅ Historical performance data
- ✅ Ability to detect anomalies
- ✅ Foundation for alerting (Prometheus AlertManager)
- ✅ Data for capacity planning
- ✅ Proof that system meets SLAs

---

## Common Pitfalls

**1. Forgetting to restart Prometheus**

- Prometheus only reads config at startup
- After changing prometheus.yml, always restart

**2. Wrong endpoint in prometheus.yml**

- Must be `/actuator/prometheus`, not `/metrics`
- Check actuator configuration if 404

**3. Docker networking issues**

- Use `host.docker.internal` on Mac/Windows
- On Linux, may need to use host IP directly

**4. Over-instrumenting**

- Don't track EVERYTHING
- Focus on business-critical metrics
- Too many metrics = noise and performance impact

**5. Not using tags**

- Example: Tag fraud metrics by reason (HIGH_VALUE, CRYPTO)
- Allows drilling down: `fraud_transactions_fraudulent{reason="CRYPTO"}`

---

## Next Steps After Sprint 4

Once metrics are working, you'll have:

- ✅ Complete fraud detection system
- ✅ Persistent storage
- ✅ Real-time alerting
- ✅ Observability

**Sprint 5 Preview:** You'll train a real ML model in Python, export to ONNX, and watch your `fraud_detection_latency` metric increase (ML inference is slower than rules, but more accurate!).

Ready to make your system observable? 🎯
