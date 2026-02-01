#!/usr/bin/env python3
"""
Explore the Kaggle Credit Card Fraud Detection Dataset.
This script helps you understand the data before training.
"""

import pandas as pd
import numpy as np
import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend (faster, no display needed)
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

# Configuration
DATA_PATH = Path("data/creditcard.csv")
FIGURES_PATH = Path("data/figures")
FIGURES_PATH.mkdir(exist_ok=True)

def load_data():
    """Load the credit card fraud dataset."""
    print("=" * 70)
    print("Loading dataset...")
    print("=" * 70)
    
    if not DATA_PATH.exists():
        print(f"❌ Dataset not found at {DATA_PATH}")
        print("\nPlease download from:")
        print("https://www.kaggle.com/datasets/mlg-ulb/creditcardfraud")
        print(f"\nAnd place creditcard.csv in the {DATA_PATH.parent} directory")
        return None
    
    df = pd.read_csv(DATA_PATH)
    print(f"✅ Loaded {len(df):,} transactions")
    return df


def basic_info(df):
    """Display basic dataset information."""
    print("\n" + "=" * 70)
    print("BASIC DATASET INFO")
    print("=" * 70)
    
    print(f"\nShape: {df.shape[0]:,} rows × {df.shape[1]} columns")
    print(f"Memory usage: {df.memory_usage(deep=True).sum() / 1024**2:.2f} MB")
    
    print("\nColumn data types:")
    print(df.dtypes.value_counts())
    
    print("\nMissing values:")
    missing = df.isnull().sum()
    if missing.sum() == 0:
        print("✅ No missing values!")
    else:
        print(missing[missing > 0])
    
    print("\nFirst few rows:")
    print(df.head())
    
    print("\nStatistical summary:")
    print(df.describe())


def class_distribution(df):
    """Analyze fraud vs legitimate transaction distribution."""
    print("\n" + "=" * 70)
    print("CLASS DISTRIBUTION (Fraud vs Legitimate)")
    print("=" * 70)
    
    class_counts = df['Class'].value_counts()
    class_percentages = df['Class'].value_counts(normalize=True) * 100
    
    print(f"\nLegitimate (0): {class_counts[0]:,} ({class_percentages[0]:.3f}%)")
    print(f"Fraudulent (1): {class_counts[1]:,} ({class_percentages[1]:.3f}%)")
    
    imbalance_ratio = class_counts[0] / class_counts[1]
    print(f"\n⚠️  Imbalance ratio: {imbalance_ratio:.1f}:1")
    print("   (This is why we need SMOTE or class weights!)")
    
    # Visualize
    plt.figure(figsize=(15, 5))
    
    # Linear scale (shows the massive imbalance)
    plt.subplot(1, 3, 1)
    class_counts.plot(kind='bar', color=['green', 'red'])
    plt.title('Transaction Counts (Linear Scale)')
    plt.xlabel('Class')
    plt.ylabel('Count')
    plt.xticks([0, 1], ['Legitimate', 'Fraudulent'], rotation=0)
    # Add count labels on bars
    for i, v in enumerate(class_counts):
        plt.text(i, v, f'{v:,}', ha='center', va='bottom')
    
    # Log scale (makes fraud visible)
    plt.subplot(1, 3, 2)
    class_counts.plot(kind='bar', color=['green', 'red'], logy=True)
    plt.title('Transaction Counts (Log Scale)')
    plt.xlabel('Class')
    plt.ylabel('Count (log scale)')
    plt.xticks([0, 1], ['Legitimate', 'Fraudulent'], rotation=0)
    plt.grid(axis='y', alpha=0.3)
    
    # Percentages
    plt.subplot(1, 3, 3)
    class_percentages.plot(kind='bar', color=['green', 'red'])
    plt.title('Transaction Percentages')
    plt.xlabel('Class')
    plt.ylabel('Percentage (%)')
    plt.xticks([0, 1], ['Legitimate', 'Fraudulent'], rotation=0)
    # Add percentage labels
    for i, v in enumerate(class_percentages):
        plt.text(i, v, f'{v:.3f}%', ha='center', va='bottom')
    
    plt.tight_layout()
    plt.savefig(FIGURES_PATH / 'class_distribution.png', dpi=150)
    print(f"\n📊 Saved visualization: {FIGURES_PATH / 'class_distribution.png'}")
    plt.close()


def time_analysis(df):
    """Analyze transaction time patterns."""
    print("\n" + "=" * 70)
    print("TIME ANALYSIS")
    print("=" * 70)
    
    print(f"\nTime range: {df['Time'].min():.0f} - {df['Time'].max():.0f} seconds")
    print(f"Duration: {df['Time'].max() / 3600:.1f} hours (~{df['Time'].max() / 86400:.1f} days)")
    
    # Convert to hours (as integers 0-23)
    df['Hour'] = ((df['Time'] / 3600) % 24).astype(int)
    
    print("\nTransactions by hour:")
    hour_dist = df.groupby('Hour').size()
    print(hour_dist.describe())
    
    # Sample data for faster plotting (use 10% for visualization)
    df_sample = df.sample(n=min(30000, len(df)), random_state=42)
    
    # Compare fraud vs legitimate by hour
    plt.figure(figsize=(12, 5))
    
    plt.subplot(1, 2, 1)
    df_sample[df_sample['Class'] == 0]['Hour'].hist(bins=24, alpha=0.7, color='green', label='Legitimate')
    df_sample[df_sample['Class'] == 1]['Hour'].hist(bins=24, alpha=0.7, color='red', label='Fraudulent')
    plt.xlabel('Hour of Day')
    plt.ylabel('Transaction Count (sampled)')
    plt.title('Transaction Distribution by Hour')
    plt.legend()
    
    plt.subplot(1, 2, 2)
    fraud_rate_by_hour = df.groupby('Hour')['Class'].mean() * 100
    fraud_rate_by_hour.plot(kind='bar', color='orange')
    plt.xlabel('Hour of Day')
    plt.ylabel('Fraud Rate (%)')
    plt.title('Fraud Rate by Hour')
    plt.xticks(rotation=45)
    
    plt.tight_layout()
    plt.savefig(FIGURES_PATH / 'time_analysis.png', dpi=150)
    print(f"📊 Saved visualization: {FIGURES_PATH / 'time_analysis.png'}")
    plt.close()


def amount_analysis(df):
    """Analyze transaction amounts."""
    print("\n" + "=" * 70)
    print("AMOUNT ANALYSIS")
    print("=" * 70)
    
    print("\nLegitimate transactions:")
    print(df[df['Class'] == 0]['Amount'].describe())
    
    print("\nFraudulent transactions:")
    print(df[df['Class'] == 1]['Amount'].describe())
    
    # Visualize
    plt.figure(figsize=(14, 5))
    
    plt.subplot(1, 3, 1)
    df[df['Class'] == 0]['Amount'].hist(bins=50, alpha=0.7, color='green', label='Legitimate')
    df[df['Class'] == 1]['Amount'].hist(bins=50, alpha=0.7, color='red', label='Fraudulent')
    plt.xlabel('Amount ($)')
    plt.ylabel('Frequency')
    plt.title('Amount Distribution (All)')
    plt.legend()
    plt.xlim(0, 500)  # Zoom to see better
    
    plt.subplot(1, 3, 2)
    df[df['Class'] == 0]['Amount'].apply(np.log1p).hist(bins=50, alpha=0.7, color='green', label='Legitimate')
    df[df['Class'] == 1]['Amount'].apply(np.log1p).hist(bins=50, alpha=0.7, color='red', label='Fraudulent')
    plt.xlabel('Log(Amount + 1)')
    plt.ylabel('Frequency')
    plt.title('Amount Distribution (Log Scale)')
    plt.legend()
    
    plt.subplot(1, 3, 3)
    df.boxplot(column='Amount', by='Class', figsize=(6, 5))
    plt.xlabel('Class (0=Legitimate, 1=Fraud)')
    plt.ylabel('Amount ($)')
    plt.title('Amount Distribution by Class')
    plt.suptitle('')  # Remove automatic title
    
    plt.tight_layout()
    plt.savefig(FIGURES_PATH / 'amount_analysis.png', dpi=150)
    print(f"📊 Saved visualization: {FIGURES_PATH / 'amount_analysis.png'}")
    plt.close()


def feature_correlation(df):
    """Analyze feature correlations with fraud."""
    print("\n" + "=" * 70)
    print("FEATURE CORRELATION WITH FRAUD")
    print("=" * 70)
    
    # Calculate correlation with Class
    correlations = df.corr()['Class'].drop('Class').sort_values(ascending=False)
    
    print("\nTop 10 positively correlated features:")
    print(correlations.head(10))
    
    print("\nTop 10 negatively correlated features:")
    print(correlations.tail(10))
    
    # Visualize top correlated features
    plt.figure(figsize=(12, 6))
    
    top_features = pd.concat([correlations.head(5), correlations.tail(5)])
    colors = ['red' if x > 0 else 'blue' for x in top_features.values]
    top_features.plot(kind='barh', color=colors)
    plt.xlabel('Correlation with Fraud')
    plt.title('Top Features Correlated with Fraud')
    plt.axvline(x=0, color='black', linestyle='--', linewidth=0.5)
    
    plt.tight_layout()
    plt.savefig(FIGURES_PATH / 'feature_correlation.png', dpi=150)
    print(f"\n📊 Saved visualization: {FIGURES_PATH / 'feature_correlation.png'}")
    plt.close()
    
    return correlations


def summary_insights(df, correlations):
    """Provide actionable insights for model training."""
    print("\n" + "=" * 70)
    print("KEY INSIGHTS FOR MODEL TRAINING")
    print("=" * 70)
    
    fraud_count = df['Class'].sum()
    fraud_rate = df['Class'].mean() * 100
    
    print(f"\n1. CLASS IMBALANCE:")
    print(f"   - Fraud rate: {fraud_rate:.3f}%")
    print(f"   - Only {fraud_count} frauds out of {len(df):,} transactions")
    print(f"   ⚠️  Action: Use SMOTE or class_weight='balanced'")
    
    print(f"\n2. IMPORTANT FEATURES:")
    top_5 = correlations.abs().sort_values(ascending=False).head(5)
    for feature, corr in top_5.items():
        print(f"   - {feature}: correlation = {corr:.3f}")
    print(f"   ✅ Action: Focus on these features in your model")
    
    print(f"\n3. EVALUATION METRICS:")
    print(f"   ❌ Don't use: Accuracy (misleading with imbalance)")
    print(f"   ✅ Use: Precision, Recall, F1-Score, ROC-AUC")
    
    print(f"\n4. DATA SPLITTING:")
    print(f"   ✅ Use: Stratified split (maintain fraud ratio in train/test)")
    print(f"   ✅ Or: Time-based split (train on early, test on late)")
    
    print(f"\n5. EXPECTED MODEL PERFORMANCE:")
    print(f"   - Target Recall: >85% (catch most fraud)")
    print(f"   - Target Precision: >80% (minimize false alarms)")
    print(f"   - Target ROC-AUC: >0.95")


def main():
    """Run full data exploration."""
    print("🔍 CREDIT CARD FRAUD DETECTION - DATA EXPLORATION")
    
    # Load data
    df = load_data()
    if df is None:
        return
    
    # Run analyses
    basic_info(df)
    class_distribution(df)
    time_analysis(df)
    amount_analysis(df)
    correlations = feature_correlation(df)
    summary_insights(df, correlations)
    
    print("\n" + "=" * 70)
    print("✅ EXPLORATION COMPLETE!")
    print("=" * 70)
    print(f"\n📁 Visualizations saved in: {FIGURES_PATH}")
    print("\n🚀 Next steps:")
    print("   1. Review the visualizations in data/figures/")
    print("   2. Understand the key insights above")
    print("   3. Proceed to Task 4: Train ML Model")
    print("=" * 70)


if __name__ == "__main__":
    main()
