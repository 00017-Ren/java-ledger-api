package com.hendrik.javaledgerapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standard error body returned for every documented failure response.")
public record ErrorResponse(
        @Schema(description = "Time the error was generated.", example = "2026-08-19T09:15:30")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code.", example = "404")
        int status,

        @Schema(description = "HTTP status reason phrase.", example = "Not Found")
        String error,

        @Schema(description = "Human-readable error detail.", example = "Account not found")
        String message,

        @Schema(description = "Request path that produced the error.",
                example = "/api/v1/accounts/8f14e45f-ceea-467e-adc9-15e5a4c1c6e6")
        String path
) {
}
