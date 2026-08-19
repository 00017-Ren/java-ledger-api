package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        @Schema(description = "Unique transaction identifier.", example = "b6c1e6b0-6e2e-4f0a-9f0e-2c8e8f6c2a10")
        UUID id,

        @Schema(description = "Debited account number. Null for a deposit, which has no source "
                + "account.", example = "400123456789", nullable = true)
        String sourceAccountNumber,

        @Schema(description = "Credited account number.", example = "400987654321")
        String destinationAccountNumber,

        @Schema(description = "Transaction amount, with 4 decimal places.", example = "125.5000")
        BigDecimal amount,

        @Schema(description = "Kind of transaction.", example = "TRANSFER")
        TransactionType type,

        @Schema(description = "Processing outcome. Every transaction created today is COMPLETED; "
                + "PENDING and FAILED are reserved for future use.", example = "COMPLETED")
        TransactionStatus status,

        @Schema(description = "Optional free-text note.", example = "Rent payment", nullable = true)
        String description,

        @Schema(description = "Timestamp the transaction was recorded.", example = "2026-08-19T09:15:30")
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
