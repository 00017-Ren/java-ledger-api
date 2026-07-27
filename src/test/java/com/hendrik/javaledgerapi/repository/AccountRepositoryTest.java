package com.hendrik.javaledgerapi.repository;

import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.hendrik.javaledgerapi.model.enums.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase
@Testcontainers
class AccountRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    User testUser = new User();
    Account testAccount = new Account();

    @BeforeEach
    void setUp() {
        testUser.setEmail("test@mail.com");
        testUser.setPasswordHash("000000");
        testUser.setRole(USER);
        userRepository.save(testUser);

        testAccount.setUser(testUser);
        testAccount.setAccountNumber("100023456000");
        testAccount.setBalance(BigDecimal.valueOf(0.0));
        testAccount.setCurrency("ZAR");
        accountRepository.save(testAccount);
    }

    @Test
    void findByUserId_returnsAccounts_whenUserIdExists() {

        List<Account> foundAccounts = accountRepository.findByUserId(testUser.getId());

        assertThat(foundAccounts).hasSize(1);
        assertThat(foundAccounts.get(0).getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void findByAccountNumber_returnsAccount_whenAccountNumberExists() {

        Optional<Account> foundAccounts = accountRepository.findByAccountNumber(testAccount.getAccountNumber());

        assertThat(foundAccounts).isPresent();
        assertThat(foundAccounts.get().getAccountNumber()).isEqualTo(testAccount.getAccountNumber());
    }

    @Test
    void existsByAccountNumber_returnsTrue_whenAccountNumberExists() {

        boolean exists = accountRepository.existsByAccountNumber("100023456000");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByAccountNumber_returnsFalse_whenAccountNumberDoesNotExists() {
        boolean exists = accountRepository.existsByAccountNumber("999999999999");

        assertThat(exists).isFalse();
    }
}
