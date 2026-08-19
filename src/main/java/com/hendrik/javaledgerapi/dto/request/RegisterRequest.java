package com.hendrik.javaledgerapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "Email address used as the login identifier. Must be unique.",
                example = "jane.doe@example.com")
        @Email @NotBlank @Size(max = 255) String email,

        @Schema(description = "Plaintext password, 8-72 characters. Stored only as a BCrypt hash.",
                example = "Str0ngPassw0rd!")
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
