package com.fraud.consumer.service;

import com.fraud.common.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;

@Slf4j
@Service
public class FeatureEngineeringService {
    public float[] extractFeatures(Transaction transaction){
        float[] features = new float[7];


        features[0] = transaction.getAmount().floatValue() / 10000.0f;

        features[1] = transaction.getTimestamp().getHour();

        //fraud patterns differ in times -> 3am transaction can be fraudulent as they are sus
        features[2] = transaction.getTimestamp().getDayOfWeek().getValue();

        //weekend has different fraud rates
        DayOfWeek dayOfWeek = transaction.getTimestamp().getDayOfWeek();
        features[3] = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) ? 1.0f : 0.0f;

        features[4] = encodePaymentMethod(transaction.getPaymentMethod());

        features[5] = Math.abs(transaction.getMerchantId().hashCode() % 1000);

        features[6] = Math.abs(transaction.getLocation().hashCode() % 1000);

        log.debug("Extracted features for transaction {}: {}",
                transaction.getTransactionID(), features);

        return features;
    }


    private float encodePaymentMethod(String paymentMethod) {
        switch (paymentMethod) {
            case "CREDIT_CARD":
                return 0.0f;
            case "DEBIT_CARD":
                return 1.0f;
            case "PAYPAL":
                return 2.0f;
            case "CRYPTO":
                return 3.0f;
            default:
                return 4.0f;
        }
}


}