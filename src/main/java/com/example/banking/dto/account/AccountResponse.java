package com.example.banking.dto.account;

import com.example.banking.enums.AccountStatus;
import com.example.banking.enums.AccountType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class AccountResponse {
    Long accountId;
    Long customerId;
    AccountType accountType;
    BigDecimal balance;
    AccountStatus status;
    LocalDateTime createdAt;
}
