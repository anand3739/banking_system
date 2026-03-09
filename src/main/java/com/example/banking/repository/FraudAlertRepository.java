package com.example.banking.repository;

import com.example.banking.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findAllByOrderByFlaggedAtDesc();
}
