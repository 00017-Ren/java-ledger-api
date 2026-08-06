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
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        Transaction transaction = new Transaction();
        transaction.setDestinationAccount(account);
        transaction.setAmount(request.amount());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(request.description());

        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransactionResponse.from(savedTransaction);
    }

    @Transactional
    public TransactionResponse transfer(UUID userId, TransferRequest request) {
        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            throw new InvalidTransferException("Source and destination account cannot be the same.");
        }

        Account sourceAccount = accountRepository.findByAccountNumber(request.sourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!userId.equals(sourceAccount.getUser().getId())) {
            throw new ResourceNotFoundException("Account not found.");
        }

        Account destinationAccount = accountRepository.findByAccountNumber(request.destinationAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!sourceAccount.getCurrency().equals(destinationAccount.getCurrency())) {
            throw new InvalidTransferException("Source and destination account must have the same currency.");
        }

        if (sourceAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds.");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.amount()));
        destinationAccount.setBalance(destinationAccount.getBalance().add(request.amount()));

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        Transaction transaction = new Transaction();
        transaction.setSourceAccount(sourceAccount);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setAmount(request.amount());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(request.description());

        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransactionResponse.from(savedTransaction);
    }

    @Transactional
    public Page<TransactionResponse> getAccountHistory(UUID accountId, UUID userId, Pageable pageable) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (userId.equals(account.getUser().getId())) {
            return transactionRepository.findByAccountId(accountId, pageable)
                    .map(TransactionResponse::from);
        }

        throw new ResourceNotFoundException("Account not found");
    }

    @Transactional
    public TransactionResponse getTransactionById(UUID transactionId, UUID userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        boolean matchSourceAccount = transaction.getSourceAccount() != null &&
                transaction.getSourceAccount().getUser().getId().equals(userId);
        boolean matchDestinationAccount = transaction.getDestinationAccount() != null &&
                transaction.getDestinationAccount().getUser().getId().equals(userId);

        if (matchSourceAccount || matchDestinationAccount) {
            return TransactionResponse.from(transaction);
        }

        throw new ResourceNotFoundException("Transaction not found");
    }
}
