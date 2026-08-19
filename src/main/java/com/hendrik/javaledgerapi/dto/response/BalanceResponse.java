package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record BalanceResponse(
        @Schema(description = "Current balance, with 4 decimal places.", example = "1250.0000")
        BigDecimal balance,

        @Schema(description = "ISO 4217 currency code.", example = "USD")
        String currency
) {
    public static BalanceResponse from(Account account) {
        return new BalanceResponse(
                account.getBalance(),
                account.getCurrency()
        );
    }
}
