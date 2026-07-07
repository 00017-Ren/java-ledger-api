package com.hendrik.javaledgerapi.dto.response;

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
}
