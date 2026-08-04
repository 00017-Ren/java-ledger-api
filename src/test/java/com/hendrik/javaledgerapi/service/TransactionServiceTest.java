package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.dto.request.TransferRequest;
import com.hendrik.javaledgerapi.exception.InsufficientFundsException;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.hendrik.javaledgerapi.model.enums.Role.USER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private UUID userId;
    private UUID userId2;
    private User testUser =  new User();
    private User testUser2 =  new User();
    private Account sourceAccount = new Account();
    private Account destinationAccount = new Account();

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sourceAccountId = UUID.randomUUID();
        destinationAccountId = UUID.randomUUID();

        testUser = new User(
                userId,
                "test@mail.com",
                "000000",
                USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        testUser2 = new User(
                userId2,
                "test2@mail.com",
                "000000",
                USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );



        sourceAccountId = UUID.randomUUID();
        sourceAccount = new Account(
                sourceAccountId,
                testUser,
                "123456789012",
                BigDecimal.ZERO,
                "ZAR",
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        destinationAccountId = UUID.randomUUID();
        destinationAccount = new Account(
                destinationAccountId,
                testUser2,
                "123456789013",
                BigDecimal.ZERO,
                "ZAR",
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }


    @Test
    void transfer_throwInsufficientFundsException_whenAccountBalanceInsufficient() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getAccountNumber(),
                destinationAccount.getAccountNumber(),
                BigDecimal.TEN,
                "transfer");

        when(accountRepository.findByAccountNumber(request.sourceAccountNumber())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(request.destinationAccountNumber())).thenReturn(Optional.of(destinationAccount));

        //TransactionResponse response = transactionService.transfer(userId, request);

        assertThatThrownBy(() -> transactionService.transfer(userId, request))
            .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
