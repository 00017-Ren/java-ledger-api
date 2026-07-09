package com.hendrik.javaledgerapi.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validate_returnsNoViolation_whenAllFieldsValid() {
        LoginRequest newLogin = new LoginRequest("test@domain.com", "21345678");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(newLogin);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void validate_returnsViolation_whenEmailInvalid(String invalidEmail) {
        LoginRequest newLoginRequest = new LoginRequest(invalidEmail, "12345678");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(newLoginRequest);
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void validate_returnsViolation_whenPasswordInalid(String invalidPassword) {
        LoginRequest newLoginRequest = new LoginRequest("test@domain.com", invalidPassword);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(newLoginRequest);
        assertThat(violations).isNotEmpty();
    }
}
