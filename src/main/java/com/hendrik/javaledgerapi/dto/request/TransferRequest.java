package com.hendrik.javaledgerapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @Schema(description = "12-digit account number debited by the transfer. Must belong to "
                + "the authenticated caller.", example = "400123456789")
        @NotBlank @Size(min = 12, max = 12) String sourceAccountNumber,

        @Schema(description = "12-digit account number credited by the transfer. Must use the "
                + "same currency as the source account.", example = "400987654321")
        @NotBlank @Size(min = 12, max = 12) String destinationAccountNumber,

        @Schema(description = "Transfer amount. Must be positive with at most 4 decimal places, "
                + "and no greater than the source account's balance.", example = "125.5000")
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,

        @Schema(description = "Optional free-text note.", example = "Rent payment")
        @Size(max = 255) String description
) {
}
