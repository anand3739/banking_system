package com.example.banking.dto.fraud;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FraudAlertResponse {
    Long alertId;
    Long transactionId;
    String fraudReason;
    Integer riskScore;
    LocalDateTime flaggedAt;
}
