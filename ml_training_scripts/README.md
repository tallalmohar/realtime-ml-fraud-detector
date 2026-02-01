# ML Training Scripts

This folder contains Python scripts used for **one-time model training and ONNX export**. These scripts are **not needed during runtime** - they're only used to generate the `fraud_model.onnx` file.

## Files

### 1. `train_model.py` ⭐ MAIN TRAINING SCRIPT

- Trains Random Forest classifier on Credit Card Fraud dataset
- Uses V1-V28 PCA features + Amount + Time
- Outputs: `fraud_model.pkl` (scikit-learn model)
- **Run this first** to train the model

**Usage:**

```bash
python ml_training_scripts/train_model.py
```

---

### 2. `export_onnx.py` ⭐ MAIN EXPORT SCRIPT

- Converts `fraud_model.pkl` → `fraud_model.onnx`
- Places ONNX file in `fraud-consumer/src/main/resources/`
- **Run this after training** to prepare model for Java

**Usage:**

```bash
python ml_training_scripts/export_onnx.py
```

---

### 3. `explore_data.py`

- Exploratory Data Analysis (EDA) on fraud dataset
- Generates statistics, visualizations
- Helps understand data distribution
- **Optional** - for data analysis only

---

### 4. `verify_setup.py`

- Verifies Python environment setup
- Checks if required packages are installed
- Tests ONNX Runtime compatibility
- **Optional** - debugging tool

---

### 5. `train_custom_features.py`

- Experimental: Train with custom feature engineering
- Alternative to standard training
- **Optional** - for experimentation

---

## Quick Start: Train and Export Model

```bash
# 1. Activate Python virtual environment
source ml-env/bin/activate

# 2. Train the model (generates fraud_model.pkl)
python ml_training_scripts/train_model.py

# 3. Export to ONNX (generates fraud_model.onnx in fraud-consumer resources)
python ml_training_scripts/export_onnx.py

# 4. Start Java fraud-consumer - it will load the ONNX model
```

---

## Why These Scripts Are Separate from Java

✅ **One-time use**: Training happens once (or periodically), not per-transaction  
✅ **Different environment**: Python has best ML tools (scikit-learn, pandas)  
✅ **ONNX bridges the gap**: Python trains → Java executes  
✅ **Clean separation**: ML research vs production deployment

---

## Output Locations

| Script           | Output File        | Destination                          |
| ---------------- | ------------------ | ------------------------------------ |
| `train_model.py` | `fraud_model.pkl`  | `models/` directory                  |
| `export_onnx.py` | `fraud_model.onnx` | `fraud-consumer/src/main/resources/` |

---

## When to Re-run Training

🔄 **Retrain the model when:**

- New fraud patterns emerge (model drift)
- Accumulate more labeled data
- Quarterly/monthly model refresh
- Fraud detection precision drops

📦 **Re-export ONNX when:**

- Model is retrained
- Switching to different ML algorithm
- Updating ONNX Runtime version

---

## Dependencies

See `requirements.txt` in project root or install manually:

```bash
pip install scikit-learn pandas numpy onnx onnxruntime skl2onnx
```
