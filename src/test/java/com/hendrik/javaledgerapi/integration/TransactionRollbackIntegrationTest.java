package com.hendrik.javaledgerapi.integration;

import com.hendrik.javaledgerapi.dto.request.TransferRequest;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import com.hendrik.javaledgerapi.service.TransactionService;
import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRollbackIntegrationTest extends PostgresIntegrationTest {
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @Sql(
            scripts = "/sql/rollback-constraint-setup.sql",
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    @Sql(
            scripts = "/sql/rollback-constraint-teardown.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
    )
    @Test
    void transactionRollback_rollsBackEntireTransaction_whenExceptionThrown() {
        User sourceUser = testDataFactory.persistUser(Role.USER);
        Account sourceAccount = testDataFactory.persistAccount(sourceUser, BigDecimal.TEN);
        Account destinationAccount = testDataFactory.persistAccount(sourceUser, BigDecimal.ONE);
        Transaction initialDeposit = testDataFactory.persistCompletedDepositTransaction(destinationAccount, BigDecimal.ONE);

        TransferRequest request = new TransferRequest(
                sourceAccount.getAccountNumber(),
                destinationAccount.getAccountNumber(),
                BigDecimal.TWO,
                "Deposit"
        );

        assertThatThrownBy(() -> transactionService.transfer(sourceUser.getId(), request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining("test_transactions_destination_account_description_unique");

        assertThat(accountRepository.findByAccountNumber(sourceAccount.getAccountNumber())
                .get()
                .getBalance()
                .compareTo(BigDecimal.TEN)).isZero();
        assertThat(accountRepository.findByAccountNumber(destinationAccount.getAccountNumber())
                .get()
                .getBalance()
                .compareTo(BigDecimal.ONE)).isZero();
        assertThat(transactionRepository.findByAccountId(sourceAccount.getId(), Pageable.unpaged())).isEmpty();
        assertThat(transactionRepository.findByAccountId(destinationAccount.getId(), Pageable.unpaged()).getContent())
                .singleElement()
                .extracting(Transaction::getId)
                .isEqualTo(initialDeposit.getId());
    }
}
