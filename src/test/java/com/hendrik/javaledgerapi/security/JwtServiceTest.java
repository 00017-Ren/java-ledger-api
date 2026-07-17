package com.hendrik.javaledgerapi.security;


import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final long DEFAULT_EXPIRATION_TIME = 3600;
    private static final long SHORT_EXPIRATION_TIME = 5;
    private static final String TEST_SECRET = "test-secret-key-at-least-32chars";

    private UserPrincipal principal;

    @BeforeEach
    void setup() {
        principal = new UserPrincipal(new User(
                UUID.randomUUID(),
                "test@mail.com",
                "000xxx000",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()));
    }

    @Test
    void generate_returnsExpectedClaims_whenPassedValidToken() {
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, DEFAULT_EXPIRATION_TIME);
        JwtService jwtService = new JwtService(jwtProperties);

        String token = jwtService.generateAccessToken(principal);

        Claims claims = jwtService.extractClaims(token);

        assertThat(token).isNotEmpty();
        assertThat(claims.get("sub")).isEqualTo(principal.getEmail());
        assertThat(claims.get("userId")).isEqualTo(principal.getId().toString());
        assertThat(claims.get("role")).isEqualTo(Role.USER.name());
    }

    @Test
    void validate_returnsTrue_whenTokenIsValid() {
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, DEFAULT_EXPIRATION_TIME);
        JwtService jwtService = new JwtService(jwtProperties);

        String token = jwtService.generateAccessToken(principal);

        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validate_returnsFalse_whenTokenIsExpired() throws InterruptedException {
        JwtProperties jwtProperties = new JwtProperties("test-secret-key-at-least-32chars", SHORT_EXPIRATION_TIME);

        JwtService jwtService = new JwtService(jwtProperties);

        String token = jwtService.generateAccessToken(principal);
        Thread.sleep(20);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validate_returnsFalse_whenTokenStringIsNotValid() {
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, DEFAULT_EXPIRATION_TIME);
        JwtService jwtService = new JwtService(jwtProperties);

        String token = jwtService.generateAccessToken(principal);

        int tokenSignatureIndex = token.lastIndexOf(".");

        String tamperedToken = token.substring(0, tokenSignatureIndex + 1) +
                "0" + token.substring(tokenSignatureIndex + 2);

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
    }
}
