package com.hendrik.javaledgerapi.support;

import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import com.hendrik.javaledgerapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestConfiguration.class)
public abstract class PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    protected TestDataFactory testDataFactory;

    @BeforeEach
    void setup() {
        testDataFactory = new TestDataFactory(userRepository, accountRepository, transactionRepository);
    }

    @AfterEach
    void tearDown() {
        testDataFactory.cleanupCreatedData();
    }
}
