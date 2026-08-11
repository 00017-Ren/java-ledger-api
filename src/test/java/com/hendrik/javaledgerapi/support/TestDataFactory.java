package com.hendrik.javaledgerapi.support;

import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import com.hendrik.javaledgerapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class TestDataFactory {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private static final AtomicLong ACCOUNT_NUMBER_SEQUENCE = new AtomicLong(1);
    private final Set<UUID> createdUserIds = new LinkedHashSet<>();
    private final Set<UUID> createdAccountIds = new LinkedHashSet<>();

    public TestDataFactory(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public User persistUser(Role callerRole) {
        User newUser = new User();
        newUser.setEmail(UUID.randomUUID() + "@tester.com");
        newUser.setPasswordHash("replace-me-hash");
        newUser.setRole(callerRole);

        User savedUser = userRepository.save(newUser);

        createdUserIds.add(savedUser.getId());

        return savedUser;
    }

    public Account persistAccount(User owner, BigDecimal balance) {
        String accountNumber = "%012d".formatted(ACCOUNT_NUMBER_SEQUENCE.getAndIncrement());

        Account newAccount = new Account();
        newAccount.setUser(owner);
        newAccount.setAccountNumber(accountNumber);
        newAccount.setBalance(balance);
        newAccount.setCurrency("ZAR");

        Account savedAccount = accountRepository.save(newAccount);

        createdAccountIds.add(savedAccount.getId());

        return savedAccount;
    }

    public Transaction persistCompletedDepositTransaction(Account destinationAccount, BigDecimal amount) {
        Transaction newTransaction = new Transaction();

        newTransaction.setDestinationAccount(destinationAccount);
        newTransaction.setAmount(amount);
        newTransaction.setType(TransactionType.DEPOSIT);
        newTransaction.setStatus(TransactionStatus.COMPLETED);
        newTransaction.setDescription("Deposit");

        return transactionRepository.save(newTransaction);
    }

    private Set<UUID> getTestAccountIds() {
        Set<UUID> testAccountIds = new LinkedHashSet<>(createdAccountIds);

        for (UUID userId : createdUserIds) {
            List<Account> userAccounts = accountRepository.findByUserId(userId);
            for (Account account : userAccounts) {
                testAccountIds.add(account.getId());
            }
        }

        return testAccountIds;
    }

    private Set<UUID> getTestTransactionIds(Set<UUID> testAccountIds) {
        Set<UUID> testTransactionIds = new LinkedHashSet<>();

        for (UUID accountId : testAccountIds) {
            Page<Transaction> transactions = transactionRepository.findByAccountId(accountId, Pageable.unpaged());
            for (Transaction transaction : transactions.getContent()) {
                testTransactionIds.add(transaction.getId());
            }
        }

        return testTransactionIds;
    }

    public void cleanupCreatedData() {
        Set<UUID> testAccountIds = getTestAccountIds();
        Set<UUID> testTransactionIds = getTestTransactionIds(testAccountIds);

        transactionRepository.deleteAllById(testTransactionIds);
        accountRepository.deleteAllById(testAccountIds);
        userRepository.deleteAllById(createdUserIds);

        createdAccountIds.clear();
        createdUserIds.clear();
    }
}
