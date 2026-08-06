package com.hendrik.javaledgerapi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank @Size(min = 12, max = 12) String sourceAccountNumber,

        @NotBlank @Size(min = 12, max = 12) String destinationAccountNumber,

        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,

        @Size(max = 255) String description
) {
}
