# Sprint 5: The ML Pipeline Bridge (Python to Java)

## Overview

In Sprint 5, you'll transition from **rule-based fraud detection** to **real machine learning**. Right now, your system flags transactions based on simple rules (amount > $900 or CRYPTO payment). This works, but it's:

- **Too rigid**: Misses nuanced patterns
- **High false positives**: Legitimate large transactions get flagged
- **Not adaptive**: Can't learn from new fraud patterns

**With machine learning, you get:**

- Pattern recognition across multiple features
- Probabilistic predictions (not just yes/no)
- Ability to detect sophisticated fraud
- Continuous improvement from new data

**The challenge:** You have a **Java application** but ML models are typically trained in **Python**. This sprint builds the bridge between the two worlds.

---

## Why This Sprint Matters

**The Business Case:**

- Rule-based: 70% accuracy, 30% false positive rate
- ML-based: 95%+ accuracy, <5% false positive rate
- Saves millions in fraud losses and customer frustration

**The Technical Challenge:**
Your system has two parts that need to work together:

1. **Python** (training): Pandas, Scikit-Learn, data science tools
2. **Java** (production): Spring Boot, real-time inference, low latency

**The solution: ONNX (Open Neural Network Exchange)**

- Universal ML model format
- Train in Python, run anywhere
- Supported by Microsoft, Facebook, AWS, Google
- Industry standard for ML deployment

---

## Architecture: Before vs After

**Current (Rule-Based):**

```
Transaction → FraudDetectionService
                ↓
            detectFraudUsingRules()
                ↓ (if amount > 900 OR payment = CRYPTO)
            return true/false
```

**After Sprint 5 (ML-Based):**

```
Transaction → FraudDetectionService
                ↓
            extractFeatures() (7 features)
                ↓
            ONNX Model (loaded at startup)
                ↓
            Neural network inference
                ↓
            return fraud probability (0.0 - 1.0)
                ↓ (if > 0.5 threshold)
            return true/false
```

**Key difference:** Instead of hard-coded rules, the model has **learned patterns** from 284,807 real credit card transactions.

---

## Task 1: Understand the ML Workflow

### 1.1: The Big Picture

**Why start with understanding?**
You're about to bridge two worlds (Python data science + Java engineering). Understanding the full workflow prevents confusion later.

**The ML Pipeline (what you'll build):**

**Phase 1: Data Science (Python)**

```
Raw Data → Clean → Engineer Features → Train Model → Export ONNX
```

**Phase 2: Engineering (Java)**

```
Load ONNX → Transaction → Extract Same Features → Inference → Prediction
```

**Critical insight:** Feature engineering MUST match exactly between training and inference. If Python uses 7 features in specific order, Java must provide the same 7 features in the same order.

---

### 1.2: Why ONNX Instead of Alternatives?

**Option 1: Call Python from Java (❌)**

- Spawn Python process for each prediction
- Extremely slow (100ms+ per prediction)
- Deployment nightmare (need Python runtime in production)
- Memory overhead

**Option 2: Reimplement Model in Java (❌)**

- Manually code RandomForest logic
- Prone to errors (did you implement it exactly like Scikit-Learn?)
- Breaks when model updates
- Months of engineering work

**Option 3: ONNX (✅)**

- Train in Python, export to universal format
- Fast native Java inference (<5ms)
- Industry standard (Microsoft, Meta, AWS use it)
- Model updates = just replace .onnx file
- No Python dependency in production

**How ONNX works:**

- Converts trained model to computational graph
- Stores weights, activation functions, layer structure
- ONNX Runtime reads graph and executes efficiently
- Language-agnostic (works in Java, C++, JavaScript, etc.)

---

### 1.3: Dataset Overview - Kaggle Credit Card Fraud

**What dataset are you using?**
Kaggle's "Credit Card Fraud Detection" dataset - industry standard for learning fraud detection.

**Key characteristics:**

- **Size**: 284,807 transactions
- **Fraud rate**: 0.172% (492 frauds) - realistic imbalance
- **Features**: Already anonymized with PCA (V1-V28)
- **Time**: Seconds elapsed between transactions
- **Amount**: Transaction value

**Why this dataset is perfect:**

- Real-world fraud distribution (highly imbalanced)
- Pre-processed (privacy compliant)
- Well-documented
- Benchmarked (you can compare your results)

**The imbalance problem:**

- 99.83% legitimate transactions
- 0.17% fraudulent transactions
- **Naive model that always predicts "not fraud" = 99.83% accuracy but useless!**
- You need techniques to handle this (covered in training task)

---

## Task 2: Set Up Python Environment

### 2.1: Why Separate Python Environment?

**The problem:**
Your Mac has a system Python with various packages. Installing ML libraries globally can:

- Conflict with system packages
- Break other projects
- Clutter your global environment
- Make deployment inconsistent

**The solution: Virtual Environment**

- Isolated Python installation
- Project-specific package versions
- Easy to delete and recreate
- Matches production environment

**Think of it like:**

- System Python = Your Mac's default apps
- Virtual env = A separate user account with its own apps

---

### 2.2: Install Required Python Libraries

**What you need and why:**

**1. Pandas**

- Purpose: Data manipulation and analysis
- Why: Load CSV, filter data, create training sets
- Example: `df[df['Class'] == 1]` finds all fraudulent transactions

**2. Scikit-Learn**

- Purpose: Machine learning library
- Why: Train RandomForest, evaluate model, split data
- Example: `RandomForestClassifier()` creates your model

**3. Imbalanced-Learn**

- Purpose: Handle imbalanced datasets
- Why: Your data is 99.83% legitimate, need techniques like SMOTE
- Example: Generates synthetic fraud examples to balance dataset

**4. ONNX & skl2onnx**

- Purpose: Export Scikit-Learn models to ONNX format
- Why: Bridge between Python training and Java inference
- Example: `convert_sklearn()` turns your model into .onnx file

**5. Matplotlib/Seaborn (optional)**

- Purpose: Visualization
- Why: Plot confusion matrix, ROC curve, feature importance
- Example: See which features matter most

---

### 2.3: Verify Installation

**Why verification matters:**
Don't start training only to discover missing dependencies 2 hours later. Test everything works before proceeding.

**What to test:**

- Import all libraries (no errors)
- Check versions (compatibility)
- Load sample data (file I/O works)
- Create tiny model (basic functionality)

---

## Task 3: Download & Explore Dataset

### 3.1: Get Kaggle Dataset

**Where to download:**
https://www.kaggle.com/datasets/mlg-ulb/creditcardfraud

**What you'll get:**

- `creditcard.csv` (150MB)
- 284,807 rows
- 31 columns (Time, V1-V28, Amount, Class)

**File structure:**

```
Time,V1,V2,...,V28,Amount,Class
0,-1.359807,-0.072781,...,149.62,0
406,-2.312227,1.951992,...,2.69,0
...
```

**Class column:**

- 0 = Legitimate transaction
- 1 = Fraudulent transaction

---

### 3.2: Explore the Data (Understand Before Training)

**Why exploration matters:**
Never train a model on data you don't understand. Garbage in = garbage out.

**What to check:**

**1. Data Quality**

- Missing values? (NaNs break training)
- Data types correct? (strings vs numbers)
- Outliers? (fraud transactions often are outliers - that's the point!)

**2. Class Distribution**

- How many fraud vs legitimate?
- Imbalance ratio?
- This determines your strategy (can't use accuracy as metric!)

**3. Feature Distributions**

- Which features differ between fraud/legitimate?
- Are features normalized? (PCA features already are)
- Any obvious patterns?

**4. Correlations**

- Which features correlate with fraud?
- Are features independent? (RandomForest handles correlation well)

**Tools for exploration:**

- `df.info()` - data types, missing values
- `df.describe()` - statistics (mean, std, min, max)
- `df['Class'].value_counts()` - fraud vs legitimate count
- `df.corr()` - feature correlations

**How this fits your system:**
Understanding the data tells you:

- What features to extract in Java
- What threshold to use (0.5? 0.3? depends on fraud rate)
- What accuracy to expect

---

## Task 4: Train Machine Learning Model

### 4.1: Choose Algorithm - RandomForest vs XGBoost

**RandomForest (Recommended for Sprint 5):**

- **How it works**: Creates 100+ decision trees, each trained on random data subset, votes on prediction
- **Pros**: Fast training, handles imbalanced data well, interpretable (feature importance)
- **Cons**: Larger model size, slower inference than single tree
- **Use case**: Great for fraud detection - robust to outliers

**XGBoost (Advanced Alternative):**

- **How it works**: Builds trees sequentially, each correcting errors of previous
- **Pros**: Highest accuracy, built-in handling of imbalanced data
- **Cons**: Slower training, more hyperparameters to tune
- **Use case**: When you need that extra 2% accuracy

**For this sprint: Start with RandomForest**

- Easier to train
- Faster experimentation
- Good enough for production (many companies use it)

---

### 4.2: Handle Class Imbalance

**The problem:**
With 99.83% legitimate transactions, a naive model learns: "Always predict legitimate, be right 99.83% of the time!"

**Techniques to handle imbalance:**

**1. SMOTE (Synthetic Minority Oversampling)**

- Creates synthetic fraud examples
- Interpolates between existing fraud transactions
- Balances training set (50/50 fraud/legitimate)
- Most common approach

**2. Class Weights**

- Penalize model more for missing fraud
- `class_weight='balanced'` in Scikit-Learn
- Model learns fraud is more important

**3. Undersampling**

- Randomly remove legitimate transactions
- Faster training
- Risk: lose information from legitimate transactions

**Recommended:** Use SMOTE - generates more training data without losing information.

---

### 4.3: Split Data (Train/Validation/Test)

**Why three splits?**

**Training Set (70%):**

- Model learns patterns from this
- Used to fit model weights

**Validation Set (15%):**

- Tune hyperparameters (tree depth, number of trees)
- Prevent overfitting
- Not used in final evaluation

**Test Set (15%):**

- Final evaluation
- Model has NEVER seen this data
- Represents real-world performance

**Critical rule:** NEVER use test set during training or tuning. It must remain unseen to give honest accuracy estimate.

**Time-based split for fraud:**
Instead of random split, use chronological:

- Train on first 70% of time
- Validate on next 15%
- Test on final 15%

**Why?** Fraud patterns evolve. You want to test: "Can model trained on past detect future fraud?"

---

### 4.4: Train the Model

**The training process:**

**Step 1: Initialize Model**

```
RandomForestClassifier(
    n_estimators=100,      # 100 trees
    max_depth=10,          # Prevent overfitting
    random_state=42        # Reproducibility
)
```

**Step 2: Fit on Training Data**

- Model builds 100 decision trees
- Each tree learns from random data subset
- Takes 1-5 minutes depending on hardware

**Step 3: Validate Performance**

- Run on validation set
- Check metrics (not just accuracy!)
- Tune hyperparameters if needed

**What's happening during training:**

- Each tree asks questions: "Is V1 > 0.5?"
- Splits data based on answers
- Learns which features predict fraud
- Stores decision rules

**How this fits your Java app:**
Once trained, these decision rules are frozen. Your Java app just executes them - no learning happens in production.

---

### 4.5: Evaluate Model (Beyond Accuracy)

**Why accuracy is misleading:**
Model predicting "always legitimate" = 99.83% accurate but catches ZERO fraud!

**Better metrics:**

**1. Precision**

- Of transactions flagged as fraud, how many were actually fraud?
- High precision = fewer false alarms
- Formula: True Positives / (True Positives + False Positives)

**2. Recall**

- Of actual fraud, how many did we catch?
- High recall = catch most fraud
- Formula: True Positives / (True Positives + False Negatives)

**3. F1-Score**

- Harmonic mean of precision and recall
- Balances both concerns
- Formula: 2 _ (Precision _ Recall) / (Precision + Recall)

**4. ROC-AUC**

- Area Under Receiver Operating Characteristic curve
- Measures model's ability to separate classes
- 0.5 = random guessing, 1.0 = perfect

**Target metrics for fraud detection:**

- Recall > 85% (catch most fraud)
- Precision > 80% (minimize false alarms)
- F1-Score > 0.80
- ROC-AUC > 0.95

---

### 4.6: Feature Importance Analysis

**Why it matters:**
Understanding WHICH features drive predictions helps you:

- Trust the model (is it using sensible features?)
- Debug issues (if model fails, check those features)
- Optimize (maybe you only need top 5 features?)

**RandomForest provides feature importance:**

- Each feature gets a score (0-1)
- Higher = more important for predictions
- Based on how much each feature reduces impurity

**Example insights:**

- If V14 has importance 0.25 → It's crucial for fraud detection
- If Amount has importance 0.02 → Transaction amount barely matters

**How this fits your Java app:**
You must extract the same features in the same order. Feature importance tells you which are critical - mess those up and model fails.

---

## Task 5: Export Model to ONNX Format

### 5.1: Why ONNX Export Isn't Trivial

**The challenge:**
Scikit-Learn models exist as Python objects in memory. Java can't read Python objects.

**What ONNX does:**

1. Inspects your trained model
2. Extracts decision tree structures
3. Converts to computational graph (nodes and edges)
4. Serializes to binary file
5. Includes metadata (input shapes, output types)

**The conversion process:**

- `skl2onnx` library reads RandomForest internals
- Translates each tree to ONNX operators
- Validates conversion (tests predictions match)
- Writes `.onnx` file

---

### 5.2: Define Input Schema

**Critical step:** Tell ONNX what inputs to expect.

**Your input schema:**

```
[
    ('float_input', FloatTensorType([None, 7]))
]
```

**Breaking it down:**

- `'float_input'` - Name of input (Java will use this)
- `FloatTensorType` - Data type (32-bit floats)
- `[None, 7]` - Shape: any number of rows, 7 columns
  - `None` = batch size (can predict 1 or 1000 transactions)
  - `7` = number of features

**Why this matters:**
If you train on 7 features but export expecting 10, Java crashes with dimension mismatch.

---

### 5.3: Document Feature Order (CRITICAL)

**The most common bug in ML deployment:**
Training uses features in order: [V1, V2, Amount, V3, V4, V5, V6]
Java provides features in order: [Amount, V1, V2, V3, V4, V5, V6]
Model gets garbage input, predicts randomly, everyone is confused.

**Solution: Document the exact order**

Create `feature_order.txt`:

```
Feature 0: normalized_amount
Feature 1: hour_of_day
Feature 2: day_of_week
Feature 3: is_weekend
Feature 4: payment_method_encoded
Feature 5: merchant_hash
Feature 6: location_hash
```

**This MUST match your Java FeatureEngineeringService EXACTLY.**

**How to enforce:**

1. Write down feature order when training
2. Unit test Java feature extraction to match
3. Include feature order in model documentation
4. Test with known inputs (predict on specific transaction, verify output)

---

### 5.4: Test ONNX Model in Python First

**Why test before deploying:**
If model works in Python but fails in Java, is it:

- ONNX conversion bug?
- Java inference bug?
- Feature extraction bug?

**Pre-deployment test:**

1. Load .onnx file in Python using ONNX Runtime
2. Run inference on test data
3. Compare predictions to original Scikit-Learn model
4. Verify they match (within rounding error)

**If predictions differ:**

- ONNX conversion failed
- Fix before moving to Java
- Check skl2onnx version compatibility

**If predictions match:**

- ONNX file is good
- Any Java issues are in inference code, not model

---

## Task 6: Integrate ONNX Model into Java

### 6.1: Move .onnx File to Resources

**Where it goes:**

```
fraud-consumer/src/main/resources/fraud_model.onnx
```

**Why this location:**

- Resources folder is packaged in JAR
- Accessible via classpath at runtime
- OnnxModelConfig already looks here (Sprint 2)

**File size check:**

- RandomForest with 100 trees: 5-20MB
- If > 50MB, consider reducing trees
- Larger = slower startup, more memory

---

### 6.2: Enable ONNX Model in Configuration

**Remember Sprint 2?**
You disabled ONNX loading:

```
onnx.model.enabled=false
```

**Now enable it:**

```
onnx.model.enabled=true
```

**What happens:**

- Spring Boot sees property = true
- OnnxModelConfig creates OrtSession bean
- Loads fraud_model.onnx at startup
- FraudDetectionService receives non-null session
- Switches from rule-based to ML-based detection

---

### 6.3: Update FeatureEngineeringService (If Needed)

**Check your features:**
Your current FeatureEngineeringService extracts:

1. normalized_amount
2. hour_of_day
3. day_of_week
4. is_weekend
5. payment_method_encoded
6. merchant_hash
7. location_hash

**If your ML model expects DIFFERENT features:**

- You need to retrain OR
- Update FeatureEngineeringService to match

**Most likely scenario:** Your features are fine, just verify ORDER matches training.

---

### 6.4: Verify detectFraudUsingModel() Logic

**Review the method:**
This was written in Sprint 2 but never used (returned false).

**What it should do:**

1. Extract features (7 floats)
2. Convert to 2D array (batch of 1)
3. Create ONNX tensor
4. Run inference
5. Extract fraud probability
6. Compare to threshold (0.5)
7. Clean up tensors (prevent memory leak)

**Key check:**

- Input shape: `[1, 7]` (one transaction, 7 features)
- Output shape: `[1, 2]` (one prediction, 2 probabilities: [legitimate_prob, fraud_prob])
- Use fraud_prob (index 1) for decision

---

## Task 7: Testing & Validation

### 7.1: Smoke Test - Does It Run?

**Basic test:**

1. Start consumer with ONNX enabled
2. Check logs: "FraudDetectionService initialized with ML MODEL detection"
3. No crashes? Good sign!

**If crashes:**

- Model file not found? Check path
- ONNX runtime error? Check model format
- Tensor shape mismatch? Check feature count

---

### 7.2: Functional Test - Correct Predictions?

**Test with known fraud:**

1. Create transaction: Amount=$5000, CRYPTO, Late night
2. Check logs: Should detect fraud (high probability)
3. Verify: Saved to database, alert sent

**Test with known legitimate:**

1. Create transaction: Amount=$50, CREDIT_CARD, Daytime
2. Check logs: Should pass (low probability)
3. Verify: NOT saved to fraud table

**If predictions seem random:**

- Feature order mismatch (most likely)
- Wrong model file loaded
- Feature engineering bug

---

### 7.3: Performance Test - Latency Impact

**Watch your Grafana dashboard:**

- Before: Rule-based latency ~0.1ms (p99)
- After: ML latency ~3-10ms (p99)

**ML is slower because:**

- 100 trees to evaluate
- Matrix operations
- More computation than simple if/else

**Acceptable latency:**

- p50: < 5ms
- p95: < 15ms
- p99: < 30ms

**If too slow:**

- Reduce number of trees (100 → 50)
- Use smaller model
- Cache predictions for repeat transactions

---

### 7.4: Accuracy Test - Real-World Performance

**Run for 1000+ transactions:**

- What's the fraud rate? (~3-5% based on your rules before)
- How many frauds detected?
- Any false positives?

**Compare to rule-based:**

- Rule-based: Caught all CRYPTO + high-value
- ML-based: Should catch those PLUS subtle patterns

**Monitor metrics:**

- `fraud_transaction_fraudulent_total` - Should still increment
- `fraud_transaction_latency` - Should increase but stay reasonable
- Fraud rate % - May change (ML is more nuanced)

---

### 7.5: Database Verification

**Check saved frauds:**

```sql
SELECT detection_method, fraud_probability, flag_reason, COUNT(*)
FROM fraudulent_transactions
GROUP BY detection_method, fraud_probability, flag_reason;
```

**What to verify:**

- `detection_method` = "ML_MODEL" (update from "RULE_BASED")
- `fraud_probability` = actual values (0.6, 0.85, 0.95) not 100.0
- `flag_reason` = "ML_HIGH_PROBABILITY" (or similar)

---

## Task 8: Update Detection Method & Metadata

### 8.1: Change Detection Method String

**Why it matters:**
You're now using ML, not rules. Database should reflect this.

**Update in FraudPersistenceService:**

- Change "RULE_BASED" → "ML_MODEL"
- Use actual fraud probability from model (not 100f)

**Update in FraudDetectionService:**

- Change flag_reason from "HIGH_VALUE" → "ML_HIGH_PROBABILITY"
- Include actual probability in logs

---

### 8.2: Threshold Tuning

**Current threshold: 0.5**

- Probability > 0.5 → Fraud
- Probability ≤ 0.5 → Legitimate

**Why tune:**

- Higher threshold (0.7): Fewer false positives, might miss fraud
- Lower threshold (0.3): Catch more fraud, more false alarms

**How to find optimal:**

1. Collect 1000 predictions with probabilities
2. Try different thresholds
3. Calculate precision/recall for each
4. Choose based on business needs

**Business question:**

- Cost of missing fraud: $1000
- Cost of false alarm: $10
- Optimize for recall (catch fraud) over precision

---

## Common Issues & Solutions

### Issue 1: "Model not found"

**Cause:** .onnx file not in resources folder
**Solution:** Check path, rebuild JAR

### Issue 2: "Tensor shape mismatch"

**Cause:** Feature count wrong
**Solution:** Verify extractFeatures() returns 7 values, model expects 7

### Issue 3: "Predictions all 0.5"

**Cause:** Model not actually running, using default
**Solution:** Check USE_RULE_BASED_DETECTION flag

### Issue 4: "High latency (>100ms)"

**Cause:** Model too large or cold start
**Solution:** Reduce trees, warm up model at startup

### Issue 5: "Random predictions"

**Cause:** Feature order mismatch
**Solution:** Document and test feature order rigorously

---

## Architecture: Full System Now

```
Producer (Mock Data)
    ↓
Kafka (transactions topic)
    ↓
Consumer receives Transaction
    ↓
FeatureEngineeringService.extractFeatures()
  → [7 numerical features]
    ↓
FraudDetectionService.detectFraudUsingModel()
  → OnnxModelConfig (loads .onnx at startup)
  → OrtSession.run(features)
  → Returns [legitimate_prob, fraud_prob]
  → fraud_prob > 0.5?
    ↓
If Fraud:
  → FraudPersistenceService.save() (PostgreSQL)
  → FraudAlertService.send() (Kafka fraud-alerts)
  → MetricsService.recordFraud()
    ↓
Prometheus scrapes metrics
    ↓
Grafana visualizes
```

**You now have a production-grade ML-powered fraud detection system!**

---

## Next Steps After Sprint 5

**Sprint 6: Resilience & Testing**

- Dead letter topics for failures
- Retry logic
- Integration tests
- Load testing

**Future Enhancements:**

- Model retraining pipeline
- A/B testing (rule-based vs ML)
- Feature drift detection
- Explainable AI (why was this flagged?)

---

## Key Takeaways

1. **ONNX bridges Python training and Java deployment**
2. **Feature order MUST match exactly between training and inference**
3. **ML is slower than rules but more accurate**
4. **Imbalanced data requires special handling (SMOTE)**
5. **Test in Python before deploying to Java**
6. **Monitor latency impact in production**
7. **Accuracy isn't enough - use precision/recall for imbalanced data**

Ready to train your first ML model? 🤖🚀
