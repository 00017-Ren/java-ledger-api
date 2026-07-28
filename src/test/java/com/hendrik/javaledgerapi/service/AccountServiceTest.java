package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.dto.response.AccountResponse;
import com.hendrik.javaledgerapi.dto.response.BalanceResponse;
import com.hendrik.javaledgerapi.exception.ResourceNotFoundException;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.hendrik.javaledgerapi.model.enums.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @InjectMocks
    private AccountService accountService;

    UUID userId;
    UUID accountId;
    User testUser = new User();
    Account savedAccount = new Account();

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = new User(
                userId,
                "test@mail.com",
                "000000",
                USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        accountId = UUID.randomUUID();
        savedAccount = new Account(
                accountId,
                testUser,
                "123456789012",
                BigDecimal.ZERO,
                "ZAR",
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createAccount_returnsNewAccount_whenValidDetailsSubmitted() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("123456789012");

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        Account result = accountService.createAccount(testUser.getId(), "ZAR");

        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getCurrency()).isEqualTo("ZAR");
        assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void listUserAccounts_returnsAllAccounts_whenValidDetailsSubmitted() {
        when(accountRepository.findByUserId(testUser.getId())).thenReturn(List.of(savedAccount));

        List<AccountResponse> response = accountService.listUserAccounts(testUser.getId());

        assertThat(response).hasSize(1);
        assertThat(response).isInstanceOf(List.class);
    }

    @Test
    void getAccountById_returnsAccount_whenValidDetailsSubmitted() {
        when(accountRepository.findById(savedAccount.getId())).thenReturn(Optional.of(savedAccount));

        AccountResponse response = accountService.getAccountById(savedAccount.getId(), testUser.getId());

        assertThat(response.accountNumber()).isEqualTo("123456789012");
        assertThat(response.currency()).isEqualTo("ZAR");
        assertThat(response.balance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getAccountById_throwsNewResourceNotFoundException_whenWrongOwnerProvided() {
        User wrongOwner = new User(UUID.randomUUID(),
                "other@domain.com",
                "000000",
                USER,
                LocalDateTime.now(),
                LocalDateTime.now());
        Account account = new Account();
        account.setUser(wrongOwner);

        when(accountRepository.findById(savedAccount.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.getAccountById(savedAccount.getId(), testUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAccountById_throwsResourceNotFoundException_whenAccountNotFound() {
        when(accountRepository.findById(savedAccount.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(savedAccount.getId(), testUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAccountBalance_returnsAccountBalance_whenValidDetailsSubmitted() {
        when(accountRepository.findById(savedAccount.getId())).thenReturn(Optional.of(savedAccount));

        BalanceResponse response = accountService.getAccountBalance(savedAccount.getId(), testUser.getId());

        assertThat(response.balance()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.currency()).isEqualTo("ZAR");
    }

    @Test
    void createAccount_throwsResourceNotFoundException_whenUserNotFound() {
        UUID nonExistingUserId = UUID.randomUUID();

        assertThatThrownBy(() -> accountService.createAccount(nonExistingUserId, "ZAR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
