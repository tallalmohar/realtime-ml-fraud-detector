package com.fraud.consumer.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
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


    // fraud prob threshold (if model says > 0.5 = fraud)
    private static final float FRAUD_THRESHOLD = 0.5f;
    //temp testing without real model
    private static final boolean USE_RULE_BASED_DETECTION = true;



    public FraudDetectionService(OrtSession modelSession, FeatureEngineeringService featureEngineeringService){
        this.modelSession = modelSession;
        this.environment = OrtEnvironment.getEnvironment();
        this.featureEngineeringService = featureEngineeringService;

        log.info("FraudDectionService initialized with ML model.");
    }
	
	
	public boolean isFraudulent(Transaction transaction){

		if(USE_RULE_BASED_DETECTION){
			return detectFraudUsingRules(transaction);
		}

        // future: real ML inference
        /*
        *   try {
        *       return detectFraudUsingModel(transaction);
        *    } catch (OrtException e){
        *       log.error("Error during ML inference: ", transaction.getTransactionID(),e);
        * }
        * return false;
        * */
        return false;
	}
    //temp rule based fraud detection for testing

    private boolean detectFraudUsingRules(Transaction transaction){
        if(transaction.getAmount().compareTo(BigDecimal.valueOf(900)) > 0){
            log.warn("RULE : High-Value transaction flagged: ${}", transaction.getAmount());
            return true;
        }

        if("CRYPTO".equals(transaction.getPaymentMethod())){
            log.warn("RULE: CRYPTO payment flagged");
            return true;
        }
        return false; //for everything else
    }
    /*
    *   FUTURE: real ml based fraud detection
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
    * */

    private boolean detectFraudUsingModel(Transaction transaction) throws OrtException {
        float[] features = featureEngineeringService.extractFeatures(transaction);

        //create the onnx tensor from the features
        // models expect 2d arrays (batch_size and num_features)
        float [][] input2D = new float[][]{features};
        OnnxTensor inputTensor = OnnxTensor.createTensor(environment,input2D);

        //run the inference, takes the input then runs through neural networks and returns ouput
        OrtSession.Result result = modelSession.run(
                java.util.Map.of("input",inputTensor)
        );

        //extract output (fraud prob)
        //example [0.12,0.85] = 15 % legit, 85% fraud
        float[][] output = (float[][]) result.get(0).getValue();
        float FraudProbability = output[0][1];

        log.info("ML Model Prediction for {}:  fraud_probability={}",
                transaction.getTransactionID(),FraudProbability);


        //clean up tensor to prevent mem leaks
        inputTensor.close();
        result.close();
        return FraudProbability > FRAUD_THRESHOLD;
    }
}
