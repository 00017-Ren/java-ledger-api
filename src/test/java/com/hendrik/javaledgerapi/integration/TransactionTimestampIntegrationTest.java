package com.hendrik.javaledgerapi.integration;

import com.hendrik.javaledgerapi.dto.request.DepositRequest;
import com.hendrik.javaledgerapi.dto.request.TransferRequest;
import com.hendrik.javaledgerapi.dto.response.TransactionResponse;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.service.TransactionService;
import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTimestampIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Test
    void depositAndTransferResponses_includePersistedCreationTimestamp() {
        User user = testDataFactory.persistUser(Role.USER);
        Account sourceAccount = testDataFactory.persistAccount(user, BigDecimal.TEN);
        Account destinationAccount = testDataFactory.persistAccount(user, BigDecimal.ZERO);

        TransactionResponse depositResponse = transactionService.deposit(Role.ADMIN,
                new DepositRequest(sourceAccount.getAccountNumber(), BigDecimal.ONE, "Timestamp deposit"));
        TransactionResponse transferResponse = transactionService.transfer(user.getId(),
                new TransferRequest(sourceAccount.getAccountNumber(), destinationAccount.getAccountNumber(),
                        BigDecimal.ONE, "Timestamp transfer"));

        assertThat(depositResponse.createdAt()).isNotNull();
        assertThat(transferResponse.createdAt()).isNotNull();
    }
}
