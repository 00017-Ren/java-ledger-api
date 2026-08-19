package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        @Schema(description = "Unique account identifier.", example = "8f14e45f-ceea-467e-adc9-15e5a4c1c6e6")
        UUID id,

        @Schema(description = "System-generated 12-digit account number.", example = "400123456789")
        String accountNumber,

        @Schema(description = "Current balance, with 4 decimal places.", example = "1250.0000")
        BigDecimal balance,

        @Schema(description = "ISO 4217 currency code.", example = "USD")
        String currency,

        @Schema(description = "Timestamp the account was created.", example = "2026-08-19T09:15:30")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp the account was last updated.", example = "2026-08-19T09:15:30")
        LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
