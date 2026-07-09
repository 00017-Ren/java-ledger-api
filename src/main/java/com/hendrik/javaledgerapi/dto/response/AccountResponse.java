package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,

        String accountNumber,

        BigDecimal balance,

        String currency,

        LocalDateTime createdAt,

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
