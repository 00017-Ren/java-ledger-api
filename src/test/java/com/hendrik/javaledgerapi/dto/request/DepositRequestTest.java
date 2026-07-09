package com.hendrik.javaledgerapi.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DepositRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validate_returnsNoViolation_whenAllFieldsValid() {

        DepositRequest newDeposit = new DepositRequest("100000000001", BigDecimal.valueOf(250.00), "salary");
        Set<ConstraintViolation<DepositRequest>> violations = validator.validate(newDeposit);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "too-short", "too-long-00000000"})
    void validate_returnsViolations_whenAccountNumberInvalid(String invalidAccountNumber) {

        DepositRequest newDeposit = new DepositRequest(invalidAccountNumber, BigDecimal.valueOf(250.00), "salary");
        Set<ConstraintViolation<DepositRequest>> violations = validator.validate(newDeposit);
        assertThat(violations).isNotEmpty();
    }

    static Stream<BigDecimal> invalidAmounts() {
        return Stream.of(null, BigDecimal.valueOf(0.0001), BigDecimal.ZERO, BigDecimal.valueOf(-10));
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void validate_returnsViolations_whenAmountInvalid(BigDecimal invalidAmount) {

        DepositRequest newDeposit = new DepositRequest("100000000001", invalidAmount, "salary");
        Set<ConstraintViolation<DepositRequest>> violations = validator.validate(newDeposit);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_returnsViolations_whenDescriptionTooLong() {

        DepositRequest newDeposit = new DepositRequest("100000000001", BigDecimal.valueOf(250.00), "a".repeat(256));
        Set<ConstraintViolation<DepositRequest>> violations = validator.validate(newDeposit);
        assertThat(violations).isNotEmpty();
    }

}
