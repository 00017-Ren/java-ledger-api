package com.hendrik.javaledgerapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Registered email address.", example = "jane.doe@example.com")
        @NotBlank String email,

        @Schema(description = "Account password.", example = "Str0ngPassw0rd!")
        @NotBlank String password
) {
}
