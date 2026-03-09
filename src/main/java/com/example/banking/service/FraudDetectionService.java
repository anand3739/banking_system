package com.example.banking.service;

import com.example.banking.config.FraudDetectionProperties;
import com.example.banking.entity.BankTransaction;
import com.example.banking.entity.FraudAlert;
import com.example.banking.enums.TransactionType;
import com.example.banking.repository.BankTransactionRepository;
import com.example.banking.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudDetectionProperties fraudDetectionProperties;
    private final BankTransactionRepository bankTransactionRepository;
    private final FraudAlertRepository fraudAlertRepository;

    @Transactional
    public List<FraudAlert> evaluateAndCreateAlerts(BankTransaction transaction) {
        List<String> fraudReasons = new ArrayList<>();
        int riskScore = 0;

        if (isHighValueTransaction(transaction.getAmount())) {
            fraudReasons.add("High value transaction above threshold: " + fraudDetectionProperties.getAmountThreshold());
            riskScore += 40;
        }

        Long velocityAccountId = getVelocityAccountId(transaction);
        if (velocityAccountId != null && isHighVelocity(velocityAccountId)) {
            fraudReasons.add("More than " + fraudDetectionProperties.getTxCountThreshold() + " transactions in "
                    + fraudDetectionProperties.getTxWindowMinutes() + " minutes for account " + velocityAccountId);
            riskScore += 35;
        }

        if (isTransferToBlacklistedAccount(transaction)) {
            fraudReasons.add("Transfer to blacklisted account: " + transaction.getReceiverAccountId());
            riskScore += 70;
        }

        if (fraudReasons.isEmpty()) {
            return List.of();
        }

        int normalizedRiskScore = Math.min(riskScore, 100);
        List<FraudAlert> alerts = fraudReasons.stream()
                .map(reason -> FraudAlert.builder()
                        .transaction(transaction)
                        .fraudReason(reason)
                        .riskScore(normalizedRiskScore)
                        .build())
                .toList();

        List<FraudAlert> savedAlerts = fraudAlertRepository.saveAll(alerts);
        log.warn("Fraud alert generated for transactionId={} with riskScore={} and reasons={}",
                transaction.getTransactionId(), normalizedRiskScore, fraudReasons);
        return savedAlerts;
    }

    private boolean isHighValueTransaction(BigDecimal amount) {
        return amount.compareTo(fraudDetectionProperties.getAmountThreshold()) > 0;
    }

    private boolean isHighVelocity(Long accountId) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(fraudDetectionProperties.getTxWindowMinutes());
        long recentTxCount = bankTransactionRepository.countRecentTransactionsForAccount(accountId, windowStart);
        return recentTxCount > fraudDetectionProperties.getTxCountThreshold();
    }

    private boolean isTransferToBlacklistedAccount(BankTransaction transaction) {
        return transaction.getTransactionType() == TransactionType.TRANSFER
                && transaction.getReceiverAccountId() != null
                && fraudDetectionProperties.getBlacklistedAccountIds().contains(transaction.getReceiverAccountId());
    }

    private Long getVelocityAccountId(BankTransaction transaction) {
        if (transaction.getSenderAccountId() != null) {
            return transaction.getSenderAccountId();
        }
        return transaction.getReceiverAccountId();
    }
}
