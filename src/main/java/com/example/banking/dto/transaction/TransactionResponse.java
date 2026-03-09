package com.example.banking.dto.transaction;

import com.example.banking.enums.TransactionStatus;
import com.example.banking.enums.TransactionType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class TransactionResponse {
    Long transactionId;
    Long senderAccountId;
    Long receiverAccountId;
    BigDecimal amount;
    TransactionType transactionType;
    LocalDateTime transactionTime;
    TransactionStatus status;
}
