package com.example.banking.service;

import com.example.banking.dto.fraud.FraudAlertResponse;
import com.example.banking.entity.FraudAlert;
import com.example.banking.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;

    @Transactional(readOnly = true)
    public List<FraudAlertResponse> getAllFraudAlerts() {
        return fraudAlertRepository.findAllByOrderByFlaggedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FraudAlertResponse toResponse(FraudAlert alert) {
        return FraudAlertResponse.builder()
                .alertId(alert.getAlertId())
                .transactionId(alert.getTransaction().getTransactionId())
                .fraudReason(alert.getFraudReason())
                .riskScore(alert.getRiskScore())
                .flaggedAt(alert.getFlaggedAt())
                .build();
    }
}
