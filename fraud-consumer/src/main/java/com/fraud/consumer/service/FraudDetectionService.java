package com.fraud.consumer.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.fraud.consumer.model.FraudResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.fraud.common.model.Transaction;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Service
public class FraudDetectionService {
	private final OrtSession modelSession;
	private final OrtEnvironment environment;
	private final FeatureEngineeringService featureEngineeringService;

	// Fraud prob threshold (if model says > 0.5 = fraud)
	private static final float FRAUD_THRESHOLD = 0.5f;

	// For Sprint 2: Use rule-based detection (no ML model needed yet)
	// For Sprint 5: Set to false when real ML model is trained
	private final boolean USE_RULE_BASED_DETECTION;

	@Autowired
	public FraudDetectionService(@Nullable OrtSession modelSession,
			FeatureEngineeringService featureEngineeringService) {
		this.modelSession = modelSession;
		this.environment = (modelSession != null) ? OrtEnvironment.getEnvironment() : null;
		this.featureEngineeringService = featureEngineeringService;

		// Decide detection mode based on whether ML model is loaded
		this.USE_RULE_BASED_DETECTION = (modelSession == null);

		if (USE_RULE_BASED_DETECTION) {
			log.info("FraudDetectionService initialized with RULE-BASED detection (Sprint 2)");
		} else {
			log.info("FraudDetectionService initialized with ML MODEL detection (Sprint 5+)");
		}
	}

	public FraudResult isFraudulent(Transaction transaction) {
		FraudResult fraudResult = new FraudResult();
		if (USE_RULE_BASED_DETECTION) {
			return detectFraudUsingRules(transaction);
		}

		// ML MODEL detection (Sprint 5)
		try {
			return detectFraudUsingModel(transaction);
		} catch (OrtException e) {
			log.error("Error during ML inference for transaction {}: ", transaction.getTransactionID(), e);
			// Fallback to rule-based if ML fails
			return detectFraudUsingRules(transaction);
		}
	}
	// temp rule based fraud detection for testing

	private FraudResult detectFraudUsingRules(Transaction transaction) {
		FraudResult fraudResult = new FraudResult();
		if (transaction.getAmount().compareTo(BigDecimal.valueOf(900)) > 0) {
			log.warn("RULE : High-Value transaction flagged: ${}", transaction.getAmount());
			fraudResult.setFraud(true);
			fraudResult.setProbability(100f);
			fraudResult.setReason("HIGH_VALUE");
			return fraudResult;
		}

		if ("CRYPTO".equals(transaction.getPaymentMethod())) {
			log.warn("RULE: CRYPTO payment flagged");
			fraudResult.setReason("CRYPTO");
			fraudResult.setFraud(true);
			fraudResult.setProbability(100f);
			return fraudResult;
		}

		fraudResult.setFraud(false);
		fraudResult.setReason("CLEAN");
		fraudResult.setProbability(0f);
		return fraudResult; // for everything else
	}

	/**
	 * ML-based fraud detection using ONNX model (Sprint 5)
	 *
	 * Flow:
	 * 1. Extract 30 features (Time, V1-V28, Amount)
	 * 2. Wrap features in ONNX tensor
	 * 3. Run model inference
	 * 4. Extract fraud probability
	 * 5. Compare to threshold (0.5) and return result
	 */
	private FraudResult detectFraudUsingModel(Transaction transaction) throws OrtException {
		FraudResult fraudResult = new FraudResult();

		// 1. Extract 30 features from transaction
		float[] features = featureEngineeringService.extractFeatures(transaction);

		// 2. Create ONNX tensor (models expect 2D: [batch_size, num_features])
		float[][] input2D = new float[][] { features };
		OnnxTensor inputTensor = OnnxTensor.createTensor(environment, input2D);

		// 3. Run inference through the RandomForest model
		OrtSession.Result result = modelSession.run(
				java.util.Map.of("float_input", inputTensor));

		// 4. Extract output probabilities from sklearn RandomForest ONNX output
		// Structure: OnnxSequence<OnnxMap<Long, Float>> where map keys are class labels
		// (0, 1)
		OnnxValue probabilitiesValue = result.get(1);

		float fraudProbability;
		if (probabilitiesValue instanceof ai.onnxruntime.OnnxSequence) {
			ai.onnxruntime.OnnxSequence sequence = (ai.onnxruntime.OnnxSequence) probabilitiesValue;
			// Get first element from sequence (first sample's probability map)
			OnnxValue firstElement = sequence.getValue().get(0);

			if (firstElement instanceof ai.onnxruntime.OnnxMap) {
				// OnnxMap contains {class_label -> probability}
				ai.onnxruntime.OnnxMap onnxMap = (ai.onnxruntime.OnnxMap) firstElement;
				@SuppressWarnings("unchecked")
				java.util.Map<Long, Float> probMap = (java.util.Map<Long, Float>) onnxMap.getValue();
				// Key 1 = fraud class probability
				fraudProbability = probMap.getOrDefault(1L, 0.0f);
			} else {
				// Fallback: treat as tensor
				float[][] probs = (float[][]) ((OnnxTensor) firstElement).getValue();
				fraudProbability = probs[0][1];
			}
		} else if (probabilitiesValue instanceof OnnxTensor) {
			// Direct tensor output
			float[][] probs = (float[][]) ((OnnxTensor) probabilitiesValue).getValue();
			fraudProbability = probs[0][1];
		} else {
			log.warn("Unexpected ONNX output type: {}", probabilitiesValue.getClass().getName());
			fraudProbability = 0.0f;
		}

		// 5. Determine if fraud based on threshold
		boolean isFraud = fraudProbability > FRAUD_THRESHOLD;

		fraudResult.setFraud(isFraud);
		fraudResult.setProbability(fraudProbability * 100); // Convert to percentage
		fraudResult.setReason(isFraud ? "ML_HIGH_PROBABILITY" : "CLEAN");

		log.info("🤖 ML Prediction for {}: fraud_prob={}% → {}",
				transaction.getTransactionID(),
				String.format("%.2f", fraudProbability * 100),
				isFraud ? "FRAUD" : "CLEAN");

		// Clean up to prevent memory leaks
		inputTensor.close();
		result.close();

		return fraudResult;
	}
}
