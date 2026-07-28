package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.exception.DuplicateResourceException;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generateAccountNumber() {
        String accountNumber = "";
        int retries = 3;

        while (retries > 0) {
            accountNumber = generateRandomAccountNumber();

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            } else if (accountRepository.existsByAccountNumber(accountNumber)) {
                retries--;
                if (retries == 0) {
                    throw new DuplicateResourceException("Account number already exists");
                }
            }
        }

        // Practically unreachable but added to satisfy compiler
        throw new DuplicateResourceException("Unable to generate unique account number");
    }

    private String generateRandomAccountNumber() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return String.valueOf(random.nextLong(100_000_000_000L, 1_000_000_000_000L));
    }
}
