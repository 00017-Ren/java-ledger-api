package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,

        String sourceAccountNumber,

        String destinationAccountNumber,

        BigDecimal amount,

        TransactionType type,

        TransactionStatus status,

        String description,

        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSourceAccount() == null ? null : transaction.getSourceAccount().getAccountNumber(),
                transaction.getDestinationAccount() == null ? null : transaction.getDestinationAccount().getAccountNumber(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
