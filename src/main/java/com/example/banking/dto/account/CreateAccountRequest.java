package com.example.banking.dto.account;

import com.example.banking.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateAccountRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "accountType is required")
    private AccountType accountType;

    @NotNull(message = "initialBalance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "initialBalance cannot be negative")
    private BigDecimal initialBalance;
}
