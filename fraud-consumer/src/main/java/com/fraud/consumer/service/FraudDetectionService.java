package com.fraud.consumer.service;

import ai.onnxruntime.OnnxTensor;
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

		// future: real ML inference
		/*
		 * try {
		 * return detectFraudUsingModel(transaction);
		 * } catch (OrtException e){
		 * log.error("Error during ML inference: ", transaction.getTransactionID(),e);
		 * }
		 * return false;
		 */
		fraudResult.setProbability(0f);
		fraudResult.setReason("CLEAN");
		fraudResult.setFraud(false);
		return fraudResult;
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
	/*
	 * FUTURE: real ml based fraud detection
	 *
	 * this is the real implementation that will run once we trained a model
	 *
	 * flow:
	 * 1. Extract features using the FeatureEngineeringService
	 * 2. Wrap Features in ONNX tensor
	 * 3. Run model inference
	 * 4. Extract fraud prob
	 * 5. compare to threshold and return bool
	 *
	 */

	private boolean detectFraudUsingModel(Transaction transaction) throws OrtException {
		float[] features = featureEngineeringService.extractFeatures(transaction);

		// create the onnx tensor from the features
		// models expect 2d arrays (batch_size and num_features)
		float[][] input2D = new float[][] { features };
		OnnxTensor inputTensor = OnnxTensor.createTensor(environment, input2D);

		// run the inference, takes the input then runs through neural networks and
		// returns ouput
		OrtSession.Result result = modelSession.run(
				java.util.Map.of("input", inputTensor));

		// extract output (fraud prob)
		// example [0.12,0.85] = 15 % legit, 85% fraud
		float[][] output = (float[][]) result.get(0).getValue();
		float FraudProbability = output[0][1];

		log.info("ML Model Prediction for {}:  fraud_probability={}",
				transaction.getTransactionID(), FraudProbability);

		// clean up tensor to prevent mem leaks
		inputTensor.close();
		result.close();
		return FraudProbability > FRAUD_THRESHOLD;
	}
}
