#!/usr/bin/env python3
"""Verify Python ML environment is set up correctly."""

def verify_imports():
    """Test all required library imports."""
    print("Testing imports...")
    
    try:
        import pandas as pd
        print(f"✅ pandas: {pd.__version__}")
    except ImportError as e:
        print(f"❌ pandas: {e}")
        return False
    
    try:
        import sklearn
        print(f"✅ scikit-learn: {sklearn.__version__}")
    except ImportError as e:
        print(f"❌ scikit-learn: {e}")
        return False
    
    try:
        import imblearn
        print(f"✅ imbalanced-learn: {imblearn.__version__}")
    except ImportError as e:
        print(f"❌ imbalanced-learn: {e}")
        return False
    
    try:
        import onnx
        print(f"✅ onnx: {onnx.__version__}")
    except ImportError as e:
        print(f"❌ onnx: {e}")
        return False
    
    try:
        import skl2onnx
        print(f"✅ skl2onnx: {skl2onnx.__version__}")
    except ImportError as e:
        print(f"❌ skl2onnx: {e}")
        return False
    
    try:
        import onnxruntime
        print(f"✅ onnxruntime: {onnxruntime.__version__}")
    except ImportError as e:
        print(f"❌ onnxruntime: {e}")
        return False
    
    try:
        import matplotlib
        print(f"✅ matplotlib: {matplotlib.__version__}")
    except ImportError as e:
        print(f"❌ matplotlib: {e}")
        return False
    
    try:
        import seaborn
        print(f"✅ seaborn: {seaborn.__version__}")
    except ImportError as e:
        print(f"❌ seaborn: {e}")
        return False
    
    return True


def test_basic_functionality():
    """Test basic ML functionality."""
    print("\nTesting basic ML functionality...")
    
    import numpy as np
    from sklearn.ensemble import RandomForestClassifier
    
    # Create tiny dataset
    X = np.array([[1, 2], [3, 4], [5, 6], [7, 8]])
    y = np.array([0, 0, 1, 1])
    
    # Train tiny model
    model = RandomForestClassifier(n_estimators=2, max_depth=2, random_state=42)
    model.fit(X, y)
    
    # Make prediction
    prediction = model.predict([[2, 3]])
    
    print(f"✅ RandomForest trained and predicted: {prediction[0]}")
    return True


if __name__ == "__main__":
    print("=" * 60)
    print("ML Environment Verification")
    print("=" * 60)
    
    if verify_imports():
        print("\n" + "=" * 60)
        if test_basic_functionality():
            print("\n" + "=" * 60)
            print("🎉 SUCCESS! Your ML environment is ready.")
            print("=" * 60)
        else:
            print("\n❌ Basic functionality test failed.")
    else:
        print("\n❌ Import verification failed. Check your installation.")
