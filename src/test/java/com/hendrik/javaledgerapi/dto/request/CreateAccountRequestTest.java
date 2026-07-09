package com.hendrik.javaledgerapi.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAccountRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validate_returnsNoViolation_whenCurrencyValid() {

        CreateAccountRequest newRequest = new CreateAccountRequest("ZAR");
        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isEmpty();

    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "ZA" , "ZARR", "zar"})
    void validate_returnsViolation_whenCurrencyInvalid(String invalidCurrency) {

        CreateAccountRequest newRequest = new CreateAccountRequest(invalidCurrency);
        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isNotEmpty();
    }
}
