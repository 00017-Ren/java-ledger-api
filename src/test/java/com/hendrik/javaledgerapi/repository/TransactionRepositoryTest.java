package com.hendrik.javaledgerapi.repository;

import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static com.hendrik.javaledgerapi.model.enums.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@Testcontainers
class TransactionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void findByAccountId_returnsTransactions_whereAccountIsSourceOrDestination() {

        User testUser = new User();
        testUser.setEmail("test@mail.com");
        testUser.setPasswordHash("000000");
        testUser.setRole(USER);
        userRepository.save(testUser);

        Account testAccount = new Account();
        testAccount.setUser(testUser);
        testAccount.setAccountNumber("100023456000");
        testAccount.setBalance(BigDecimal.valueOf(300.0));
        testAccount.setCurrency("ZAR");
        accountRepository.save(testAccount);

        Account testAccount2 = new Account();
        testAccount2.setUser(testUser);
        testAccount2.setAccountNumber("100023456001");
        testAccount2.setBalance(BigDecimal.valueOf(500.0));
        testAccount2.setCurrency("ZAR");
        accountRepository.save(testAccount2);

        Transaction transaction1 = new Transaction();
        transaction1.setSourceAccount(testAccount);
        transaction1.setDestinationAccount(testAccount2);
        transaction1.setAmount(BigDecimal.valueOf(250.0));
        transaction1.setType(TransactionType.TRANSFER);
        transaction1.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setSourceAccount(testAccount2);
        transaction2.setDestinationAccount(testAccount);
        transaction2.setAmount(BigDecimal.valueOf(875.0));
        transaction2.setType(TransactionType.TRANSFER);
        transaction2.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction2);

        Transaction transaction3 = new Transaction();
        transaction3.setDestinationAccount(testAccount2);
        transaction3.setAmount(BigDecimal.valueOf(23000.0));
        transaction3.setType(TransactionType.DEPOSIT);
        transaction3.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction3);

        Page<Transaction> foundTransactions = transactionRepository.findByAccountId(testAccount.getId(), Pageable.unpaged());

        assertThat(foundTransactions.getContent().size()).isEqualTo(2);

    }

    @Test
    void findByAccountId_respectsPagination() {

        User testUser = new User();
        testUser.setEmail("test@mail.com");
        testUser.setPasswordHash("000000");
        testUser.setRole(USER);
        userRepository.save(testUser);

        Account testAccount = new Account();
        testAccount.setUser(testUser);
        testAccount.setAccountNumber("100023456000");
        testAccount.setBalance(BigDecimal.valueOf(300.0));
        testAccount.setCurrency("ZAR");
        accountRepository.save(testAccount);

        Account testAccount2 = new Account();
        testAccount2.setUser(testUser);
        testAccount2.setAccountNumber("100023456001");
        testAccount2.setBalance(BigDecimal.valueOf(500.0));
        testAccount2.setCurrency("ZAR");
        accountRepository.save(testAccount2);

        Transaction transaction1 = new Transaction();
        transaction1.setSourceAccount(testAccount);
        transaction1.setDestinationAccount(testAccount2);
        transaction1.setAmount(BigDecimal.valueOf(250.0));
        transaction1.setType(TransactionType.TRANSFER);
        transaction1.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setSourceAccount(testAccount2);
        transaction2.setDestinationAccount(testAccount);
        transaction2.setAmount(BigDecimal.valueOf(875.0));
        transaction2.setType(TransactionType.TRANSFER);
        transaction2.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction2);

        Transaction transaction3 = new Transaction();
        transaction3.setDestinationAccount(testAccount2);
        transaction3.setAmount(BigDecimal.valueOf(23000.0));
        transaction3.setType(TransactionType.DEPOSIT);
        transaction3.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction3);

        Transaction transaction4 = new Transaction();
        transaction4.setDestinationAccount(testAccount);
        transaction4.setAmount(BigDecimal.valueOf(17000.0));
        transaction4.setType(TransactionType.DEPOSIT);
        transaction4.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction4);

        Page<Transaction> foundTransactions = transactionRepository.findByAccountId(testAccount.getId(), PageRequest.of(0, 2));

        assertThat(foundTransactions.getTotalElements()).isEqualTo(3);
        assertThat(foundTransactions.getContent().size()).isEqualTo(2);
        assertThat(foundTransactions.hasNext()).isTrue();

    }


}
