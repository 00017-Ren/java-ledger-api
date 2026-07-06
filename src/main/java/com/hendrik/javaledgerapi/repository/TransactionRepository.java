package com.hendrik.javaledgerapi.repository;

import com.hendrik.javaledgerapi.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.sourceAccount.id = :accountId " +
            "OR t.destinationAccount.id = :accountId")
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

}
