package com.fraud.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
This POJO will be blueprint for every single transaction that flow's through the system.
LifeCyle:
    fraud-producer service (creates a new transaction) ----> send it to Kafka ---> fraud-consumer will then recieve this
    created transaction and know exactly what's inside of it because they all have the same blueprint.
 */
@Data
@NoArgsConstructor
public class Transaction {

    private String transactionID;
    private String userId;
    private BigDecimal amount; // monetary val of transaction
    private String merchantId;
    private LocalDateTime timestamp;
    private String location;
    private String paymentMethod;

    // ML features: Time + V1-V28 (PCA-transformed features from Kaggle dataset)
    // These are generated based on transaction characteristics for fraud detection
    private Float time; // Seconds since first transaction
    private Float v1;
    private Float v2;
    private Float v3;
    private Float v4;
    private Float v5;
    private Float v6;
    private Float v7;
    private Float v8;
    private Float v9;
    private Float v10;
    private Float v11;
    private Float v12;
    private Float v13;
    private Float v14;
    private Float v15;
    private Float v16;
    private Float v17;
    private Float v18;
    private Float v19;
    private Float v20;
    private Float v21;
    private Float v22;
    private Float v23;
    private Float v24;
    private Float v25;
    private Float v26;
    private Float v27;
    private Float v28;

}