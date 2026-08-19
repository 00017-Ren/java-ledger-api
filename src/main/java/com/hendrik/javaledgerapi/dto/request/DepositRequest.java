package com.hendrik.javaledgerapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepositRequest(
        @Schema(description = "12-digit account number credited by the deposit.",
                example = "400123456789")
        @NotBlank @Size(min = 12, max = 12) String destinationAccountNumber,

        @Schema(description = "Deposit amount. Must be positive with at most 4 decimal places.",
                example = "125.5000")
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,

        @Schema(description = "Optional free-text note.", example = "Payroll deposit")
        @Size(max = 255) String description
) {
}
