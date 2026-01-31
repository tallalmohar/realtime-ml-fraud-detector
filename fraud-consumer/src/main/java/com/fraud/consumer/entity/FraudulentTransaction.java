package com.fraud.consumer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="fraudulent_transactions")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FraudulentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dbTransactionID;

    private String transactionID;
    private String userId;
    private BigDecimal amount; //monetary val of transaction
    private String merchantId;
    private LocalDateTime timestamp;
    private String location;
    private String paymentMethod;

    private String detectionMethod;
    private Float fraudProbability;
    private LocalDateTime detectedAt;
    private String flagReason;


}
