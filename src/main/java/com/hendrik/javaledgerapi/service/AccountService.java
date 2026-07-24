package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.dto.response.AccountResponse;
import com.hendrik.javaledgerapi.dto.response.BalanceResponse;
import com.hendrik.javaledgerapi.exception.ResourceNotFoundException;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountNumberGenerator accountNumberGenerator;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public AccountService(
            AccountNumberGenerator accountNumberGenerator,
            UserRepository userRepository,
            AccountRepository accountRepository) {
        this.accountNumberGenerator = accountNumberGenerator;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    public Account createAccount(UUID userId, String currency) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->new ResourceNotFoundException("User not found"));

        String accountNumber = accountNumberGenerator.generateAccountNumber();

        Account account = new Account(
                userId,
                user,
                accountNumber,
                BigDecimal.valueOf(0),
                currency,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Invalid account details provided");
        }
    }

    public List<AccountResponse> listUserAccounts (UUID userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<AccountResponse> accountResponses = new ArrayList<>();

        for (Account account : accounts) {
            AccountResponse accountResponse = AccountResponse.from(account);
            accountResponses.add(accountResponse);
        }

        return accountResponses;
    }

    public AccountResponse getAccountById(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (userId.equals(account.getUser().getId())) {
            return AccountResponse.from(account);
        }

        throw new ResourceNotFoundException("Account not found");
    }

    public BalanceResponse getAccountBalance(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (userId.equals(account.getUser().getId())) {
            return BalanceResponse.from(account);
        }

        throw new ResourceNotFoundException("Account not found");
    }
}
