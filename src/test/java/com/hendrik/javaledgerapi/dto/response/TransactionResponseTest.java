package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionResponseTest {

    @Test
    void from_withNullSourceAccount_mapsToNullSourceAccountNumber() {
        Account destination = new Account(UUID.randomUUID(), null, "100000000001",
                BigDecimal.TEN, "ZAR", 0, LocalDateTime.now(), LocalDateTime.now());

        Transaction deposit = new Transaction(UUID.randomUUID(), null, destination,
                BigDecimal.TEN, TransactionType.DEPOSIT, TransactionStatus.COMPLETED,
                "test deposit", LocalDateTime.now());

        TransactionResponse response = TransactionResponse.from(deposit);

        assertThat(response.sourceAccountNumber()).isNull();
        assertThat(response.destinationAccountNumber()).isEqualTo("100000000001");
    }
}
