package com.fraud.consumer.repository;

import com.fraud.consumer.entity.FraudulentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudulentTransactionRepository extends JpaRepository<FraudulentTransaction, Long> {

}
