package com.example.banking.service;

import com.example.banking.dto.account.AccountResponse;
import com.example.banking.dto.account.BalanceResponse;
import com.example.banking.dto.account.CreateAccountRequest;
import com.example.banking.entity.Account;
import com.example.banking.entity.Customer;
import com.example.banking.enums.AccountStatus;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Customer customer = customerService.getCustomerEntity(request.getCustomerId());

        Account account = Account.builder()
                .customer(customer)
                .accountType(request.getAccountType())
                .balance(safeAmount(request.getInitialBalance()))
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Cacheable(cacheNames = "accounts", key = "#accountId")
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId) {
        Account account = findAccount(accountId);
        return toResponse(account);
    }

    @Cacheable(cacheNames = "balances", key = "#accountId")
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long accountId) {
        Account account = findAccount(accountId);
        return BalanceResponse.builder()
                .accountId(account.getAccountId())
                .balance(account.getBalance())
                .build();
    }

    @Transactional(readOnly = true)
    public Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .customerId(account.getCustomer().getCustomerId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
