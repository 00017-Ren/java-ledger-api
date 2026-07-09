package com.hendrik.javaledgerapi.dto.request;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

class RegisterRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validate_returnsNoViolations_whenAllFieldsValid() {

        RegisterRequest newRequest = new RegisterRequest("test@domain.com", "12345678");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isEmpty();

    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-an-email", "missingdomain@"})
    void validate_returnsViolation_whenEmailInvalid(String invalidEmail) {

        RegisterRequest newRequest = new RegisterRequest(invalidEmail, "12345678");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isNotEmpty();

    }

    @ParameterizedTest
    @ValueSource(strings = {"",
            // Too short
            "123456",
            // Too long
            "111112222233333444445555566666777778888899999000001111122222333334444455555"})
    void validate_returnsViolation_whenPasswordInvalid(String invalidPassword) {

        RegisterRequest newRequest = new RegisterRequest("test@domain.com", invalidPassword);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isNotEmpty();
    }


}
