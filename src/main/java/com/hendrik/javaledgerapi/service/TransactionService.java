package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.dto.request.DepositRequest;
import com.hendrik.javaledgerapi.dto.response.TransactionResponse;
import com.hendrik.javaledgerapi.exception.ResourceNotFoundException;
import com.hendrik.javaledgerapi.exception.UnauthorizedAccessException;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse deposit(Role callerRole, DepositRequest request) {
        if (callerRole != Role.ADMIN) {
            throw new UnauthorizedAccessException("Not authorized.");
        }

        Account account = accountRepository.findByAccountNumber(request.destinationAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        BigDecimal balance = account.getBalance();
        BigDecimal newBalance = balance.add(request.amount());
        account.setBalance(newBalance);

        accountRepository.save(account);

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                null,
                account,
                request.amount(),
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                request.description(),
                LocalDateTime.now()
        );

        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransactionResponse.from(savedTransaction);
    }
}
