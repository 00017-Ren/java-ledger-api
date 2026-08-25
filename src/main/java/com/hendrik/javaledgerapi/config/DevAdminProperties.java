package com.hendrik.javaledgerapi.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ledger.dev-admin")
@Validated
public record DevAdminProperties(
        boolean enabled,

        @Email @Size(max = 255) String email,

        @Size(min = 8, max = 72) String password
) {
    @AssertTrue(message = "ledger.dev-admin email and password must be non-blank when enabled")
    public boolean isCredentialsPresentWhenEnabled() {
        return !enabled || (email != null && !email.isBlank()
                && password != null && !password.isBlank());
    }
}
