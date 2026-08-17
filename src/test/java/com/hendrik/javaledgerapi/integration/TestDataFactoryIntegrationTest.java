package com.hendrik.javaledgerapi.integration;

import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import com.hendrik.javaledgerapi.repository.UserRepository;
import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestDataFactoryIntegrationTest extends PostgresIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void cleanupCreatedData_removesOnlyFactoryCreatedData() {
        User persistedUser = testDataFactory.persistUser(Role.USER);
        Account persistedAccount = testDataFactory.persistAccount(persistedUser, BigDecimal.ZERO);
        Transaction persistedTransaction = testDataFactory.persistCompletedDepositTransaction(persistedAccount, BigDecimal.TEN);

        UUID userId = persistedUser.getId();
        UUID accountId = persistedAccount.getId();
        UUID transactionId = persistedTransaction.getId();

        testDataFactory.cleanupCreatedData();

        assertThat(userRepository.findById(userId)).isNotPresent();
        assertThat(accountRepository.findById(accountId)).isNotPresent();
        assertThat(transactionRepository.findById(transactionId)).isNotPresent();
        assertThat(userRepository.findByEmail("admin@ledger.com")).isPresent();
    }
}
