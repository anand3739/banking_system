package com.example.banking.repository;

import com.example.banking.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findBySenderAccountIdOrReceiverAccountIdOrderByTransactionTimeDesc(Long senderAccountId,
                                                                                               Long receiverAccountId);

    @Query("""
            select count(t)
            from BankTransaction t
            where (t.senderAccountId = :accountId or t.receiverAccountId = :accountId)
            and t.transactionTime >= :windowStart
            """)
    long countRecentTransactionsForAccount(@Param("accountId") Long accountId,
                                           @Param("windowStart") LocalDateTime windowStart);
}
