package com.example.banking.service;

import com.example.banking.dto.transaction.DepositRequest;
import com.example.banking.dto.transaction.TransactionResponse;
import com.example.banking.dto.transaction.TransferRequest;
import com.example.banking.dto.transaction.WithdrawRequest;
import com.example.banking.entity.Account;
import com.example.banking.entity.BankTransaction;
import com.example.banking.enums.AccountStatus;
import com.example.banking.enums.TransactionStatus;
import com.example.banking.enums.TransactionType;
import com.example.banking.exception.AccountFrozenException;
import com.example.banking.exception.BadRequestException;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final FraudDetectionService fraudDetectionService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "accounts", key = "#request.accountId"),
            @CacheEvict(cacheNames = "balances", key = "#request.accountId")
    })
    public TransactionResponse deposit(DepositRequest request) {
        BigDecimal amount = normalizeAmount(request.getAmount());
        Account account = findAccountForUpdate(request.getAccountId());
        validateAccountActive(account);

        log.info("Processing DEPOSIT: accountId={}, amount={}", account.getAccountId(), amount);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        BankTransaction transaction = saveSuccessfulTransaction(
                null,
                account.getAccountId(),
                amount,
                TransactionType.DEPOSIT
        );
        fraudDetectionService.evaluateAndCreateAlerts(transaction);

        log.info("Deposit successful: transactionId={}, accountId={}, newBalance={}",
                transaction.getTransactionId(), account.getAccountId(), account.getBalance());
        return toResponse(transaction);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "accounts", key = "#request.accountId"),
            @CacheEvict(cacheNames = "balances", key = "#request.accountId")
    })
    public TransactionResponse withdraw(WithdrawRequest request) {
        BigDecimal amount = normalizeAmount(request.getAmount());
        Account account = findAccountForUpdate(request.getAccountId());
        validateAccountActive(account);
        validateSufficientFunds(account, amount);

        log.info("Processing WITHDRAW: accountId={}, amount={}", account.getAccountId(), amount);
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        BankTransaction transaction = saveSuccessfulTransaction(
                account.getAccountId(),
                null,
                amount,
                TransactionType.WITHDRAW
        );
        fraudDetectionService.evaluateAndCreateAlerts(transaction);

        log.info("Withdraw successful: transactionId={}, accountId={}, newBalance={}",
                transaction.getTransactionId(), account.getAccountId(), account.getBalance());
        return toResponse(transaction);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "accounts", key = "#request.senderAccountId"),
            @CacheEvict(cacheNames = "accounts", key = "#request.receiverAccountId"),
            @CacheEvict(cacheNames = "balances", key = "#request.senderAccountId"),
            @CacheEvict(cacheNames = "balances", key = "#request.receiverAccountId")
    })
    public TransactionResponse transfer(TransferRequest request) {
        if (request.getSenderAccountId().equals(request.getReceiverAccountId())) {
            throw new BadRequestException("Sender and receiver account cannot be the same");
        }

        BigDecimal amount = normalizeAmount(request.getAmount());
        LockedAccounts lockedAccounts = lockAccountsForTransfer(
                request.getSenderAccountId(),
                request.getReceiverAccountId()
        );

        Account sender = lockedAccounts.sender();
        Account receiver = lockedAccounts.receiver();

        validateAccountActive(sender);
        validateAccountActive(receiver);
        validateSufficientFunds(sender, amount);

        log.info("Processing TRANSFER: senderAccountId={}, receiverAccountId={}, amount={}",
                sender.getAccountId(), receiver.getAccountId(), amount);

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        BankTransaction transaction = saveSuccessfulTransaction(
                sender.getAccountId(),
                receiver.getAccountId(),
                amount,
                TransactionType.TRANSFER
        );
        fraudDetectionService.evaluateAndCreateAlerts(transaction);

        log.info("Transfer successful: transactionId={}, senderAccountId={}, receiverAccountId={}",
                transaction.getTransactionId(), sender.getAccountId(), receiver.getAccountId());
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }
        return bankTransactionRepository.findBySenderAccountIdOrReceiverAccountIdOrderByTransactionTimeDesc(
                        accountId, accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Account findAccountForUpdate(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private LockedAccounts lockAccountsForTransfer(Long senderAccountId, Long receiverAccountId) {
        Long firstAccountId = Math.min(senderAccountId, receiverAccountId);
        Long secondAccountId = Math.max(senderAccountId, receiverAccountId);

        Account firstLockedAccount = findAccountForUpdate(firstAccountId);
        Account secondLockedAccount = findAccountForUpdate(secondAccountId);

        if (senderAccountId.equals(firstAccountId)) {
            return new LockedAccounts(firstLockedAccount, secondLockedAccount);
        }
        return new LockedAccounts(secondLockedAccount, firstLockedAccount);
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Account is not active: " + account.getAccountId());
        }
    }

    private void validateSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account: " + account.getAccountId());
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BankTransaction saveSuccessfulTransaction(Long senderAccountId,
                                                      Long receiverAccountId,
                                                      BigDecimal amount,
                                                      TransactionType transactionType) {
        BankTransaction transaction = BankTransaction.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .transactionType(transactionType)
                .status(TransactionStatus.SUCCESS)
                .build();
        return bankTransactionRepository.save(transaction);
    }

    private TransactionResponse toResponse(BankTransaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .senderAccountId(transaction.getSenderAccountId())
                .receiverAccountId(transaction.getReceiverAccountId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .transactionTime(transaction.getTransactionTime())
                .status(transaction.getStatus())
                .build();
    }

    private record LockedAccounts(Account sender, Account receiver) {
    }
}
