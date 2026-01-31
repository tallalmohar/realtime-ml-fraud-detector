package com.fraud.consumer.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final MeterRegistry meterRegistry;
    private Counter transactionCounter;
    private Counter fraudulentCounter;
    private Timer fraudDetectionTimer;
    private Counter cleanTransactionCounter;

    public MetricsService(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;

        this.transactionCounter = Counter.builder("fraud.transaction.total")
                .description("Total # of transactions processed")
                .register(meterRegistry);

        this.fraudulentCounter = Counter.builder("fraud.transaction.fraudulent")
                .description("Total # of fraudulent transactions processed")
                .register(meterRegistry);

        this.fraudDetectionTimer= Timer.builder("fraud.transaction.latency")
                .description("Time take to detect fraud")
                .publishPercentiles(0.5,0.95,0.99)
                .register(meterRegistry);
        this.cleanTransactionCounter = Counter.builder("fraud.transaction.clean")
                .description("Total # of clean transaction processed")
                .register(meterRegistry);
    }

    public void recordTransaction() {
        transactionCounter.increment();
    }

    public void recordFraud() {
        fraudulentCounter.increment();
    }

    public void recordClean() {
        cleanTransactionCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDetectionTime(Timer.Sample sample) {
        sample.stop(fraudDetectionTimer);
    }




}
