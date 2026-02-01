# Feature Order for ONNX Model

**CRITICAL:** Java FeatureEngineeringService MUST provide features in this EXACT order.

## Input Schema

- **Input shape:** `[batch_size, 30]`
- **Data type:** Float32
- **Batch size:** Variable (1 for single transaction, N for batch)

## Feature List (Index Order)

```
Feature  0: Time
Feature  1: V1
Feature  2: V2
Feature  3: V3
Feature  4: V4
Feature  5: V5
Feature  6: V6
Feature  7: V7
Feature  8: V8
Feature  9: V9
Feature 10: V10
Feature 11: V11
Feature 12: V12
Feature 13: V13
Feature 14: V14
Feature 15: V15
Feature 16: V16
Feature 17: V17
Feature 18: V18
Feature 19: V19
Feature 20: V20
Feature 21: V21
Feature 22: V22
Feature 23: V23
Feature 24: V24
Feature 25: V25
Feature 26: V26
Feature 27: V27
Feature 28: V28
Feature 29: Amount
```

## Example Input

For a single transaction:
```java
float[][] input = new float[1][30];  // 1 transaction, 30 features
input[0][0] = transaction.getTime();     // Feature 0: Time
input[0][1] = transaction.getV1();       // Feature 1: V1
input[0][2] = transaction.getV2();       // Feature 2: V2
// ... continue for all 30 features ...
input[0][29] = transaction.getAmount();  // Feature 29: Amount
```

## Output Schema

- **Label output:** Integer (0 = Legitimate, 1 = Fraudulent)
- **Probability output:** Float[2] = [prob_legitimate, prob_fraudulent]
- **Use:** `probability[1]` for fraud probability (0.0 to 1.0)
- **Threshold:** If `probability[1] > 0.5`, classify as fraud

## Important Notes

1. **Feature order matters!** Swapping features will cause incorrect predictions.
2. **All 30 features required** - Time, V1-V28, Amount (in this order).
3. **Data type must be Float32** - convert all features to float.
4. **No normalization needed** - model trained on raw values from dataset.

## Model Performance

- Precision: 0.7830
- Recall: 0.8469
- F1-Score: 0.8137
- ROC-AUC: 0.9799
