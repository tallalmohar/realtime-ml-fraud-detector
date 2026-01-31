package com.fraud.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FraudResult {
    private boolean isFraud;
    private String reason;
    private Float probability;

}
