#!/usr/bin/env python3
"""
Train a RandomForest model for credit card fraud detection.
Handles class imbalance with SMOTE and evaluates with proper metrics.
"""

import pandas as pd
import numpy as np
import pickle
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    roc_auc_score,
    precision_recall_curve,
    roc_curve
)
from imblearn.over_sampling import SMOTE
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import seaborn as sns

# Configuration
DATA_PATH = Path("data/creditcard.csv")
MODEL_PATH = Path("models")
MODEL_PATH.mkdir(exist_ok=True)

# Model hyperparameters
RANDOM_STATE = 42
TEST_SIZE = 0.2
VAL_SIZE = 0.2  # 20% of training set
N_ESTIMATORS = 200  # More trees = better generalization
MAX_DEPTH = 20  # Deeper trees = capture more complex patterns
MIN_SAMPLES_SPLIT = 5  # Allow finer splits
MIN_SAMPLES_LEAF = 2  # More granular leaf nodes
CLASS_WEIGHT = 'balanced_subsample'  # Balance classes within each tree

print("=" * 70)
print("CREDIT CARD FRAUD DETECTION - MODEL TRAINING")
print("=" * 70)


def load_and_prepare_data():
    """Load dataset and prepare features/target."""
    print("\n[1/7] Loading dataset...")
    
    df = pd.read_csv(DATA_PATH)
    print(f"✅ Loaded {len(df):,} transactions")
    
    # Separate features and target
    X = df.drop(['Class'], axis=1)
    y = df['Class']
    
    print(f"   Features: {X.shape[1]} columns")
    print(f"   Fraud cases: {y.sum()} ({y.mean()*100:.3f}%)")
    
    return X, y


def split_data(X, y):
    """Split into train, validation, and test sets."""
    print("\n[2/7] Splitting data (train/val/test)...")
    
    # First split: train+val vs test
    X_temp, X_test, y_temp, y_test = train_test_split(
        X, y, 
        test_size=TEST_SIZE, 
        random_state=RANDOM_STATE,
        stratify=y  # Maintain fraud ratio
    )
    
    # Second split: train vs val
    X_train, X_val, y_train, y_val = train_test_split(
        X_temp, y_temp,
        test_size=VAL_SIZE,
        random_state=RANDOM_STATE,
        stratify=y_temp
    )
    
    print(f"✅ Training set: {len(X_train):,} samples ({y_train.sum()} frauds)")
    print(f"✅ Validation set: {len(X_val):,} samples ({y_val.sum()} frauds)")
    print(f"✅ Test set: {len(X_test):,} samples ({y_test.sum()} frauds)")
    
    return X_train, X_val, X_test, y_train, y_val, y_test


def apply_smote(X_train, y_train):
    """Apply SMOTE to balance training data."""
    print("\n[3/7] Applying SMOTE to balance training data...")
    
    print(f"   Before SMOTE:")
    print(f"   - Legitimate: {(y_train == 0).sum():,}")
    print(f"   - Fraudulent: {(y_train == 1).sum():,}")
    print(f"   - Ratio: {(y_train == 0).sum() / (y_train == 1).sum():.1f}:1")
    
    smote = SMOTE(random_state=RANDOM_STATE)
    X_train_balanced, y_train_balanced = smote.fit_resample(X_train, y_train)
    
    print(f"   After SMOTE:")
    print(f"   - Legitimate: {(y_train_balanced == 0).sum():,}")
    print(f"   - Fraudulent: {(y_train_balanced == 1).sum():,}")
    print(f"   - Ratio: 1:1 (balanced!)")
    print(f"✅ Training set size: {len(X_train_balanced):,} samples")
    
    return X_train_balanced, y_train_balanced


def train_model(X_train, y_train):
    """Train RandomForest classifier."""
    print("\n[4/7] Training RandomForest model...")
    print(f"   Hyperparameters:")
    print(f"   - n_estimators: {N_ESTIMATORS}")
    print(f"   - max_depth: {MAX_DEPTH}")
    print(f"   - min_samples_split: {MIN_SAMPLES_SPLIT}")
    print(f"   - min_samples_leaf: {MIN_SAMPLES_LEAF}")
    print(f"   - class_weight: {CLASS_WEIGHT}")
    
    model = RandomForestClassifier(
        n_estimators=N_ESTIMATORS,
        max_depth=MAX_DEPTH,
        min_samples_split=MIN_SAMPLES_SPLIT,
        min_samples_leaf=MIN_SAMPLES_LEAF,
        class_weight=CLASS_WEIGHT,
        random_state=RANDOM_STATE,
        n_jobs=-1,  # Use all CPU cores
        verbose=1
    )
    
    print("\n   Training in progress...")
    model.fit(X_train, y_train)
    
    print("✅ Model trained successfully!")
    return model


def evaluate_model(model, X_val, y_val, X_test, y_test):
    """Evaluate model on validation and test sets."""
    print("\n[5/7] Evaluating model performance...")
    
    # Validation set
    print("\n   📊 VALIDATION SET RESULTS:")
    y_val_pred = model.predict(X_val)
    y_val_proba = model.predict_proba(X_val)[:, 1]
    
    print("\n" + classification_report(y_val, y_val_pred, target_names=['Legitimate', 'Fraudulent']))
    print(f"   ROC-AUC Score: {roc_auc_score(y_val, y_val_proba):.4f}")
    
    # Test set
    print("\n   📊 TEST SET RESULTS (Final Performance):")
    y_test_pred = model.predict(X_test)
    y_test_proba = model.predict_proba(X_test)[:, 1]
    
    print("\n" + classification_report(y_test, y_test_pred, target_names=['Legitimate', 'Fraudulent']))
    
    test_roc_auc = roc_auc_score(y_test, y_test_proba)
    print(f"   ROC-AUC Score: {test_roc_auc:.4f}")
    
    # Confusion matrix
    cm = confusion_matrix(y_test, y_test_pred)
    print("\n   Confusion Matrix:")
    print(f"   TN: {cm[0,0]:,}  |  FP: {cm[0,1]:,}")
    print(f"   FN: {cm[1,0]:,}  |  TP: {cm[1,1]:,}")
    
    # Calculate metrics manually for clarity
    tn, fp, fn, tp = cm.ravel()
    precision = tp / (tp + fp)
    recall = tp / (tp + fn)
    f1 = 2 * (precision * recall) / (precision + recall)
    
    print(f"\n   ✅ Precision: {precision:.4f} (of flagged frauds, {precision*100:.2f}% are actually fraud)")
    print(f"   ✅ Recall: {recall:.4f} (caught {recall*100:.2f}% of all frauds)")
    print(f"   ✅ F1-Score: {f1:.4f}")
    print(f"   ✅ ROC-AUC: {test_roc_auc:.4f}")
    
    # Visualize confusion matrix
    plt.figure(figsize=(8, 6))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', 
                xticklabels=['Legitimate', 'Fraudulent'],
                yticklabels=['Legitimate', 'Fraudulent'])
    plt.title('Confusion Matrix (Test Set)')
    plt.ylabel('True Label')
    plt.xlabel('Predicted Label')
    plt.tight_layout()
    plt.savefig(MODEL_PATH / 'confusion_matrix.png', dpi=150)
    print(f"\n   📊 Saved: {MODEL_PATH / 'confusion_matrix.png'}")
    plt.close()
    
    # ROC Curve
    fpr, tpr, _ = roc_curve(y_test, y_test_proba)
    plt.figure(figsize=(8, 6))
    plt.plot(fpr, tpr, color='blue', lw=2, label=f'ROC curve (AUC = {test_roc_auc:.4f})')
    plt.plot([0, 1], [0, 1], color='red', lw=2, linestyle='--', label='Random classifier')
    plt.xlim([0.0, 1.0])
    plt.ylim([0.0, 1.05])
    plt.xlabel('False Positive Rate')
    plt.ylabel('True Positive Rate (Recall)')
    plt.title('ROC Curve')
    plt.legend(loc="lower right")
    plt.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig(MODEL_PATH / 'roc_curve.png', dpi=150)
    print(f"   📊 Saved: {MODEL_PATH / 'roc_curve.png'}")
    plt.close()
    
    return {
        'precision': precision,
        'recall': recall,
        'f1': f1,
        'roc_auc': test_roc_auc
    }


def analyze_feature_importance(model, feature_names):
    """Analyze and visualize feature importance."""
    print("\n[6/7] Analyzing feature importance...")
    
    # Get feature importances
    importances = model.feature_importances_
    indices = np.argsort(importances)[::-1]
    
    print("\n   Top 15 Most Important Features:")
    for i in range(min(15, len(feature_names))):
        idx = indices[i]
        print(f"   {i+1:2d}. {feature_names[idx]:15s} - {importances[idx]:.4f}")
    
    # Visualize top 20
    plt.figure(figsize=(10, 8))
    top_n = min(20, len(feature_names))
    top_indices = indices[:top_n]
    top_features = [feature_names[i] for i in top_indices]
    top_importances = importances[top_indices]
    
    plt.barh(range(top_n), top_importances, color='skyblue')
    plt.yticks(range(top_n), top_features)
    plt.xlabel('Feature Importance')
    plt.title('Top 20 Feature Importances')
    plt.gca().invert_yaxis()
    plt.tight_layout()
    plt.savefig(MODEL_PATH / 'feature_importance.png', dpi=150)
    print(f"\n   📊 Saved: {MODEL_PATH / 'feature_importance.png'}")
    plt.close()


def save_model(model, metrics, feature_names):
    """Save the trained model and metadata."""
    print("\n[7/7] Saving model...")
    
    # Save model using pickle
    model_file = MODEL_PATH / 'fraud_model.pkl'
    with open(model_file, 'wb') as f:
        pickle.dump(model, f)
    print(f"✅ Model saved: {model_file}")
    
    # Save metadata
    metadata = {
        'model_type': 'RandomForestClassifier',
        'n_estimators': N_ESTIMATORS,
        'max_depth': MAX_DEPTH,
        'features': feature_names,
        'n_features': len(feature_names),
        'metrics': metrics,
        'random_state': RANDOM_STATE
    }
    
    metadata_file = MODEL_PATH / 'model_metadata.pkl'
    with open(metadata_file, 'wb') as f:
        pickle.dump(metadata, f)
    print(f"✅ Metadata saved: {metadata_file}")
    
    # Save feature names for reference
    feature_file = MODEL_PATH / 'feature_names.txt'
    with open(feature_file, 'w') as f:
        for i, name in enumerate(feature_names):
            f.write(f"{i}: {name}\n")
    print(f"✅ Feature names saved: {feature_file}")


def main():
    """Main training pipeline."""
    
    # Load data
    X, y = load_and_prepare_data()
    feature_names = X.columns.tolist()
    
    # Split data
    X_train, X_val, X_test, y_train, y_val, y_test = split_data(X, y)
    
    # Apply SMOTE
    X_train_balanced, y_train_balanced = apply_smote(X_train, y_train)
    
    # Train model
    model = train_model(X_train_balanced, y_train_balanced)
    
    # Evaluate
    metrics = evaluate_model(model, X_val, y_val, X_test, y_test)
    
    # Feature importance
    analyze_feature_importance(model, feature_names)
    
    # Save model
    save_model(model, metrics, feature_names)
    
    print("\n" + "=" * 70)
    print("✅ TRAINING COMPLETE!")
    print("=" * 70)
    print("\n📁 Generated files:")
    print(f"   - {MODEL_PATH / 'fraud_model.pkl'} (trained model)")
    print(f"   - {MODEL_PATH / 'model_metadata.pkl'} (metrics & config)")
    print(f"   - {MODEL_PATH / 'feature_names.txt'} (feature reference)")
    print(f"   - {MODEL_PATH / 'confusion_matrix.png'}")
    print(f"   - {MODEL_PATH / 'roc_curve.png'}")
    print(f"   - {MODEL_PATH / 'feature_importance.png'}")
    
    print("\n🎯 Model Performance Summary:")
    print(f"   Precision: {metrics['precision']:.4f}")
    print(f"   Recall:    {metrics['recall']:.4f}")
    print(f"   F1-Score:  {metrics['f1']:.4f}")
    print(f"   ROC-AUC:   {metrics['roc_auc']:.4f}")
    
    if metrics['recall'] >= 0.85 and metrics['precision'] >= 0.80:
        print("\n   ✅ Model meets target performance!")
    else:
        print("\n   ⚠️  Model below target - consider tuning hyperparameters")
    
    print("\n🚀 Next step: Task 5 - Export to ONNX")
    print("=" * 70)


if __name__ == "__main__":
    main()
