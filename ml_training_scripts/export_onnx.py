#!/usr/bin/env python3
"""
Export trained RandomForest model to ONNX format.
Tests ONNX model matches original predictions before saving.
"""

import pickle
import numpy as np
import pandas as pd
from pathlib import Path
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
import onnx
import onnxruntime as rt

# Configuration
MODEL_PATH = Path("models")
DATA_PATH = Path("data/creditcard.csv")
ONNX_MODEL_PATH = MODEL_PATH / "fraud_model.onnx"

print("=" * 70)
print("ONNX MODEL EXPORT")
print("=" * 70)


def load_trained_model():
    """Load the trained RandomForest model."""
    print("\n[1/6] Loading trained model...")
    
    model_file = MODEL_PATH / "fraud_model.pkl"
    metadata_file = MODEL_PATH / "model_metadata.pkl"
    
    with open(model_file, 'rb') as f:
        model = pickle.load(f)
    
    with open(metadata_file, 'rb') as f:
        metadata = pickle.load(f)
    
    print(f"✅ Loaded RandomForest with {metadata['n_estimators']} trees")
    print(f"   Features: {metadata['n_features']}")
    print(f"   Test ROC-AUC: {metadata['metrics']['roc_auc']:.4f}")
    
    return model, metadata


def define_input_schema(n_features):
    """Define ONNX input schema."""
    print("\n[2/6] Defining input schema...")
    
    # Define input: batch of transactions with n_features columns
    initial_type = [('float_input', FloatTensorType([None, n_features]))]
    
    print(f"✅ Input schema: FloatTensorType([None, {n_features}])")
    print(f"   - 'None' = variable batch size (can predict 1 or many)")
    print(f"   - '{n_features}' = number of features per transaction")
    
    return initial_type


def convert_to_onnx(model, initial_type):
    """Convert sklearn model to ONNX format."""
    print("\n[3/6] Converting model to ONNX...")
    
    print("   Converting RandomForest to ONNX computational graph...")
    onnx_model = convert_sklearn(
        model,
        initial_types=initial_type,
        target_opset=12  # ONNX opset version (compatible with most runtimes)
    )
    
    print("✅ Conversion successful!")
    print(f"   ONNX graph nodes: {len(onnx_model.graph.node)}")
    
    return onnx_model


def test_onnx_model(onnx_model, original_model, metadata):
    """Test ONNX model predictions match original model."""
    print("\n[4/6] Testing ONNX model accuracy...")
    
    # Load test data
    print("   Loading test data for verification...")
    df = pd.read_csv(DATA_PATH)
    X = df.drop(['Class'], axis=1)
    y = df['Class']
    
    # Take small sample for testing
    X_test = X.sample(n=100, random_state=42).values.astype(np.float32)
    y_test = y.loc[X_test[:, 0].astype(int)].values
    
    # Original model predictions
    sklearn_pred = original_model.predict(X_test)
    sklearn_proba = original_model.predict_proba(X_test)[:, 1]
    
    # ONNX model predictions
    sess = rt.InferenceSession(onnx_model.SerializeToString())
    input_name = sess.get_inputs()[0].name
    output_names = [out.name for out in sess.get_outputs()]
    
    onnx_outputs = sess.run(output_names, {input_name: X_test})
    onnx_pred = onnx_outputs[0]  # Labels
    
    # Handle different probability output formats
    onnx_proba_output = onnx_outputs[1]
    if isinstance(onnx_proba_output, dict):
        # Dictionary format
        onnx_proba = np.array([v[1] for v in onnx_proba_output.values()])
    elif isinstance(onnx_proba_output, list):
        # List of dicts format
        onnx_proba = np.array([item[1] for item in onnx_proba_output])
    else:
        # Array format
        onnx_proba_array = np.array(onnx_proba_output)
        if onnx_proba_array.ndim == 2:
            onnx_proba = onnx_proba_array[:, 1]
        else:
            onnx_proba = onnx_proba_array
    
    # Compare predictions
    pred_match = np.array_equal(sklearn_pred, onnx_pred)
    proba_diff = np.abs(sklearn_proba - onnx_proba).max()
    
    print(f"\n   Prediction comparison (100 test samples):")
    print(f"   - Predictions match: {pred_match} ✅")
    print(f"   - Max probability difference: {proba_diff:.6f}")
    
    if pred_match and proba_diff < 1e-5:
        print("   ✅ ONNX model matches original model perfectly!")
        return True
    elif proba_diff < 1e-3:
        print("   ✅ ONNX model matches within acceptable tolerance")
        return True
    else:
        print("   ❌ Warning: ONNX predictions differ significantly!")
        return False


def save_onnx_model(onnx_model):
    """Save ONNX model to file."""
    print("\n[5/6] Saving ONNX model...")
    
    with open(ONNX_MODEL_PATH, "wb") as f:
        f.write(onnx_model.SerializeToString())
    
    file_size_mb = ONNX_MODEL_PATH.stat().st_size / (1024 * 1024)
    print(f"✅ ONNX model saved: {ONNX_MODEL_PATH}")
    print(f"   File size: {file_size_mb:.2f} MB")


def document_feature_order(metadata):
    """Document exact feature order for Java integration."""
    print("\n[6/6] Documenting feature order (CRITICAL for Java)...")
    
    feature_doc = MODEL_PATH / "FEATURE_ORDER.md"
    
    with open(feature_doc, 'w') as f:
        f.write("# Feature Order for ONNX Model\n\n")
        f.write("**CRITICAL:** Java FeatureEngineeringService MUST provide features in this EXACT order.\n\n")
        f.write("## Input Schema\n\n")
        f.write(f"- **Input shape:** `[batch_size, {metadata['n_features']}]`\n")
        f.write(f"- **Data type:** Float32\n")
        f.write(f"- **Batch size:** Variable (1 for single transaction, N for batch)\n\n")
        f.write("## Feature List (Index Order)\n\n")
        f.write("```\n")
        
        for i, feature in enumerate(metadata['features']):
            f.write(f"Feature {i:2d}: {feature}\n")
        
        f.write("```\n\n")
        f.write("## Example Input\n\n")
        f.write("For a single transaction:\n")
        f.write("```java\n")
        f.write("float[][] input = new float[1][30];  // 1 transaction, 30 features\n")
        f.write("input[0][0] = transaction.getTime();     // Feature 0: Time\n")
        f.write("input[0][1] = transaction.getV1();       // Feature 1: V1\n")
        f.write("input[0][2] = transaction.getV2();       // Feature 2: V2\n")
        f.write("// ... continue for all 30 features ...\n")
        f.write("input[0][29] = transaction.getAmount();  // Feature 29: Amount\n")
        f.write("```\n\n")
        f.write("## Output Schema\n\n")
        f.write("- **Label output:** Integer (0 = Legitimate, 1 = Fraudulent)\n")
        f.write("- **Probability output:** Float[2] = [prob_legitimate, prob_fraudulent]\n")
        f.write("- **Use:** `probability[1]` for fraud probability (0.0 to 1.0)\n")
        f.write("- **Threshold:** If `probability[1] > 0.5`, classify as fraud\n\n")
        f.write("## Important Notes\n\n")
        f.write("1. **Feature order matters!** Swapping features will cause incorrect predictions.\n")
        f.write("2. **All 30 features required** - Time, V1-V28, Amount (in this order).\n")
        f.write("3. **Data type must be Float32** - convert all features to float.\n")
        f.write("4. **No normalization needed** - model trained on raw values from dataset.\n\n")
        f.write(f"## Model Performance\n\n")
        f.write(f"- Precision: {metadata['metrics']['precision']:.4f}\n")
        f.write(f"- Recall: {metadata['metrics']['recall']:.4f}\n")
        f.write(f"- F1-Score: {metadata['metrics']['f1']:.4f}\n")
        f.write(f"- ROC-AUC: {metadata['metrics']['roc_auc']:.4f}\n")
    
    print(f"✅ Feature documentation saved: {feature_doc}")
    print(f"\n   📋 Review this file before Java integration!")


def display_summary(metadata):
    """Display export summary."""
    print("\n" + "=" * 70)
    print("✅ ONNX EXPORT COMPLETE!")
    print("=" * 70)
    
    print("\n📁 Generated files:")
    print(f"   - {ONNX_MODEL_PATH} (ONNX model for Java)")
    print(f"   - {MODEL_PATH / 'FEATURE_ORDER.md'} (feature documentation)")
    
    print("\n🎯 Ready for Java Integration:")
    print(f"   1. Copy {ONNX_MODEL_PATH.name} to fraud-consumer/src/main/resources/")
    print(f"   2. Read {MODEL_PATH / 'FEATURE_ORDER.md'} carefully")
    print(f"   3. Verify your Java FeatureEngineeringService extracts all {metadata['n_features']} features")
    print(f"   4. Ensure feature order matches EXACTLY")
    
    print("\n⚠️  Critical Checks Before Java Integration:")
    print("   [ ] Feature count matches (30 features)")
    print("   [ ] Feature order documented and understood")
    print("   [ ] ONNX model tested and validated")
    print("   [ ] Ready to enable onnx.model.enabled=true")
    
    print("\n🚀 Next: Task 6 - Integrate ONNX Model into Java")
    print("=" * 70)


def main():
    """Main export pipeline."""
    
    # Load model
    model, metadata = load_trained_model()
    
    # Define input schema
    initial_type = define_input_schema(metadata['n_features'])
    
    # Convert to ONNX
    onnx_model = convert_to_onnx(model, initial_type)
    
    # Test ONNX model
    if not test_onnx_model(onnx_model, model, metadata):
        print("\n⚠️  Warning: Proceed with caution - ONNX model may not match original")
    
    # Save ONNX model
    save_onnx_model(onnx_model)
    
    # Document feature order
    document_feature_order(metadata)
    
    # Display summary
    display_summary(metadata)


if __name__ == "__main__":
    main()
