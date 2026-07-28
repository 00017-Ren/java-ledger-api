package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.Account;

import java.math.BigDecimal;

public record BalanceResponse(
        BigDecimal balance,

        String currency
) {
    public static BalanceResponse from(Account account) {
        return new BalanceResponse(
                account.getBalance(),
                account.getCurrency()
        );
    }
}
