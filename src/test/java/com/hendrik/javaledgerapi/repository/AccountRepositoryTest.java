package com.hendrik.javaledgerapi.repository;

import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
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

    @Test
    void findByUserId_returnsAccounts_whenUserIdExists() {

        User testUser = new User();
        testUser.setEmail("test@mail.com");
        testUser.setPasswordHash("000000");
        testUser.setRole(USER);
        userRepository.save(testUser);

        Account testAccount = new Account();
        testAccount.setUser(testUser);
        testAccount.setAccountNumber("100023456000");
        testAccount.setBalance(BigDecimal.valueOf(0.0));
        testAccount.setCurrency("ZAR");
        accountRepository.save(testAccount);

        List<Account> foundAccounts = accountRepository.findByUserId(testUser.getId());

        assertThat(foundAccounts).hasSize(1);
        assertThat(foundAccounts.get(0).getUser().getId()).isEqualTo(testUser.getId());

    }

    @Test
    void findByAccountNumber_returnsAccount_whenAccountNumberExists() {

        User testUser = new User();
        testUser.setEmail("test@mail.com");
        testUser.setPasswordHash("000000");
        testUser.setRole(USER);
        userRepository.save(testUser);

        Account testAccount = new Account();
        testAccount.setUser(testUser);
        testAccount.setAccountNumber("100023456000");
        testAccount.setBalance(BigDecimal.valueOf(0.0));
        testAccount.setCurrency("ZAR");
        accountRepository.save(testAccount);

        Optional<Account> foundAccounts = accountRepository.findByAccountNumber(testAccount.getAccountNumber());

        assertThat(foundAccounts).isPresent();
        assertThat(foundAccounts.get().getAccountNumber()).isEqualTo(testAccount.getAccountNumber());

    }
}
