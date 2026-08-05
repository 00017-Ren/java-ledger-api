package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.dto.request.DepositRequest;
import com.hendrik.javaledgerapi.dto.request.TransferRequest;
import com.hendrik.javaledgerapi.dto.response.TransactionResponse;
import com.hendrik.javaledgerapi.exception.InsufficientFundsException;
import com.hendrik.javaledgerapi.exception.InvalidTransferException;
import com.hendrik.javaledgerapi.exception.ResourceNotFoundException;
import com.hendrik.javaledgerapi.exception.UnauthorizedAccessException;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.hendrik.javaledgerapi.model.enums.Role.ADMIN;
import static com.hendrik.javaledgerapi.model.enums.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
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
    private User testUser = new User();
    private User testUser2 = new User();
    private Account sourceAccount = new Account();
    private Account destinationAccount = new Account();
    private Role callerRole;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userId2 = UUID.randomUUID();
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

        assertThatThrownBy(() -> transactionService.transfer(userId, request))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_throwInvalidTransferException_whenUserTransfersToOwnAccount() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getAccountNumber(),
                sourceAccount.getAccountNumber(),
                BigDecimal.TEN,
                "transfer"
        );

        assertThatThrownBy(() -> transactionService.transfer(userId, request))
                .isInstanceOf(InvalidTransferException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_throws404_whenWrongOwnerForSourceAccount() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getAccountNumber(),
                destinationAccount.getAccountNumber(),
                BigDecimal.TEN,
                "transfer"
        );

        when(accountRepository.findByAccountNumber(request.sourceAccountNumber())).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> transactionService.transfer(userId2, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_throws404_whenDestinationAccountMissing() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getAccountNumber(),
                null,
                BigDecimal.TEN,
                "transfer"
        );

        when(accountRepository.findByAccountNumber(request.sourceAccountNumber())).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> transactionService.transfer(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_throwsInvalidTransferException_whenCurrencyMismatchOnAccounts() {
        destinationAccount.setCurrency("USD");
        TransferRequest request = new TransferRequest(
                sourceAccount.getAccountNumber(),
                destinationAccount.getAccountNumber(),
                BigDecimal.TEN,
                "transfer"
        );

        when(accountRepository.findByAccountNumber(request.sourceAccountNumber())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(request.destinationAccountNumber())).thenReturn(Optional.of(destinationAccount));

        assertThatThrownBy(() -> transactionService.transfer(userId, request))
                .isInstanceOf(InvalidTransferException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_returnsTransferResponse_whenTransferRequestValid() {
        sourceAccount.setBalance(BigDecimal.TEN);

        TransferRequest request = new TransferRequest(
                sourceAccount.getAccountNumber(),
                destinationAccount.getAccountNumber(),
                BigDecimal.TWO,
                "transfer"
        );

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                sourceAccount,
                destinationAccount,
                BigDecimal.TWO,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                "transfer",
                LocalDateTime.now()
        );

        when(accountRepository.findByAccountNumber(request.sourceAccountNumber())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(request.destinationAccountNumber())).thenReturn(Optional.of(destinationAccount));
        when(transactionRepository.save(any())).thenReturn(transaction);

        TransactionResponse response = transactionService.transfer(userId, request);

        assertThat(sourceAccount.getBalance()).isEqualTo(BigDecimal.valueOf(8));
        assertThat(destinationAccount.getBalance()).isEqualTo(BigDecimal.TWO);

        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void deposit_throwsUnauthorizedException_whenCallerRoleNotAdmin() {
        callerRole = USER;

        DepositRequest request = new DepositRequest(
                destinationAccount.getAccountNumber(),
                BigDecimal.TEN,
                "deposit"
        );

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                null,
                destinationAccount,
                BigDecimal.TEN,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "deposit",
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> transactionService.deposit(callerRole, request))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deposit_throws404_whenDestinationAccountMissing() {
        callerRole = ADMIN;

        DepositRequest request = new DepositRequest(
                null,
                BigDecimal.TEN,
                "deposit"
        );

        assertThatThrownBy(() -> transactionService.deposit(callerRole, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deposit_returnsTransactionResponse_whenDepositRequestValid() {
        callerRole = ADMIN;
        DepositRequest request = new DepositRequest(
                destinationAccount.getAccountNumber(),
                BigDecimal.TEN,
                "deposit"
        );

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                null,
                destinationAccount,
                BigDecimal.TEN,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "deposit",
                LocalDateTime.now()
        );

        when(accountRepository.findByAccountNumber(request.destinationAccountNumber())).thenReturn(Optional.of(destinationAccount));
        when(transactionRepository.save(any())).thenReturn(transaction);

        TransactionResponse response = transactionService.deposit(callerRole, request);

        assertThat(response.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(destinationAccount.getBalance()).isEqualTo(BigDecimal.TEN);
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void accountHistory_returnsPaginatedTransactionResponse_whenAccountHistoryRequestValid() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Transaction> transactionPage = new PageImpl<>(
                List.of(new Transaction(
                        UUID.randomUUID(),
                        sourceAccount,
                        destinationAccount,
                        BigDecimal.TEN,
                        TransactionType.TRANSFER,
                        TransactionStatus.COMPLETED,
                        "test history",
                        LocalDateTime.now()
                )),
                pageable,
                1);

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(transactionRepository.findByAccountId(sourceAccountId, pageable)).thenReturn(transactionPage);

        Page<TransactionResponse> response = transactionService.getAccountHistory(sourceAccountId, userId, pageable);

        assertThat(response.getContent())
                .singleElement()
                .satisfies(transactionResponse -> {
                    assertThat(transactionResponse.sourceAccountNumber()).isEqualTo(sourceAccount.getAccountNumber());
                    assertThat(transactionResponse.destinationAccountNumber()).isEqualTo(destinationAccount.getAccountNumber());
                    assertThat(transactionResponse.amount()).isEqualTo(BigDecimal.TEN);
                    assertThat(transactionResponse.type()).isEqualTo(TransactionType.TRANSFER);
                    assertThat(transactionResponse.status()).isEqualTo(TransactionStatus.COMPLETED);
                    assertThat(transactionResponse.description()).isEqualTo("test history");
                });

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getNumber()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getNumberOfElements()).isEqualTo(1);

        verify(transactionRepository, times(1)).findByAccountId(sourceAccountId, pageable);
    }

    @Test
    void getAccountHistory_throws404_whenWrongAccountOwnerPassed() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Transaction> transactionPage = new PageImpl<>(
                List.of(new Transaction(
                        UUID.randomUUID(),
                        sourceAccount,
                        destinationAccount,
                        BigDecimal.TEN,
                        TransactionType.TRANSFER,
                        TransactionStatus.COMPLETED,
                        "test history",
                        LocalDateTime.now()
                )),
                pageable,
                1);

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> transactionService.getAccountHistory(sourceAccountId, userId2, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTransaction_returnsTransactionResponse_whenValidSourceAccountOwner() {
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                sourceAccount,
                null,
                BigDecimal.TEN,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "deposit",
                LocalDateTime.now()
        );

        when(transactionRepository.findById(sourceAccountId)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getTransactionById(sourceAccountId, userId);

        assertThat(response.sourceAccountNumber()).isEqualTo(sourceAccount.getAccountNumber());
        assertThat(response.destinationAccountNumber()).isEqualTo(null);
        assertThat(response.amount()).isEqualTo(BigDecimal.TEN);
        assertThat(response.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.description()).isEqualTo("deposit");

        verify(transactionRepository, times(1)).findById(sourceAccountId);
    }

    @Test
    void getTransaction_returnsTransactionResponse_whenValidDestinationAccountOwner() {
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                null,
                destinationAccount,
                BigDecimal.TEN,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "deposit",
                LocalDateTime.now()
        );

        when(transactionRepository.findById(destinationAccountId)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getTransactionById(destinationAccountId, userId2);

        assertThat(response.sourceAccountNumber()).isEqualTo(null);
        assertThat(response.destinationAccountNumber()).isEqualTo(destinationAccount.getAccountNumber());
        assertThat(response.amount()).isEqualTo(BigDecimal.TEN);
        assertThat(response.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.description()).isEqualTo("deposit");

        verify(transactionRepository, times(1)).findById(destinationAccountId);
    }

    @Test
    void getTransaction_throws404_whenWrongAccountOwnerPassed() {
        User wrongUser = new User(
                UUID.randomUUID(),
                "test@mail.com",
                "000000",
                USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                null,
                destinationAccount,
                BigDecimal.TEN,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "deposit",
                LocalDateTime.now()
        );

        when(transactionRepository.findById(destinationAccountId)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.getTransactionById(destinationAccountId, wrongUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, times(1)).findById(destinationAccountId);
    }

    @Test
    void transfer_throwsUncaught409_whenOptimisticLockFailed() {
        sourceAccount.setBalance(BigDecimal.TEN);

        TransferRequest request = new TransferRequest(
                sourceAccount.getAccountNumber(),
                destinationAccount.getAccountNumber(),
                BigDecimal.TWO,
                "transfer"
        );

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                sourceAccount,
                destinationAccount,
                BigDecimal.TWO,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                "transfer",
                LocalDateTime.now()
        );

        when(accountRepository.findByAccountNumber(request.sourceAccountNumber())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber(request.destinationAccountNumber())).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(sourceAccount)).thenThrow(ObjectOptimisticLockingFailureException.class);

        assertThatThrownBy(() -> transactionService.transfer(userId,request))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(transactionRepository, never()).save(any());
    }
}
