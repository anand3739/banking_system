package com.example.banking.dto.account;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class BalanceResponse {
    Long accountId;
    BigDecimal balance;
}
