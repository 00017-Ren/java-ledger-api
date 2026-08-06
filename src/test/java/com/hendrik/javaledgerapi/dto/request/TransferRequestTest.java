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

class TransferRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validate_returnsNoViolations_whenAllFieldsValid() {
        TransferRequest newRequest = new TransferRequest("100000000001", "100000000012", BigDecimal.valueOf(250.00), "salary");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isEmpty();

    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "too-short", "too-long-00000000"})
    void validate_returnsViolations_whenSourceAccountNumberInvalid(String invalidAccountNumber) {

        TransferRequest newRequest = new TransferRequest(invalidAccountNumber, "100000000012", BigDecimal.valueOf(250.00), "salary");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "too-short", "too-long-00000000"})
    void validate_returnsViolations_whenDestinationAccountNumberInvalid(String invalidAccountNumber) {

        TransferRequest newRequest = new TransferRequest("100000000001", invalidAccountNumber, BigDecimal.valueOf(250.00), "salary");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isNotEmpty();
    }

    static Stream<BigDecimal> invalidAmounts() {
        return Stream.of(null, BigDecimal.valueOf(0.0001), BigDecimal.ZERO, BigDecimal.valueOf(-10), new BigDecimal("1.00001"));
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void validate_returnsViolations_whenAmountInvalid(BigDecimal invalidAmount) {

        TransferRequest newTransfer = new TransferRequest("100000000001", "100000000012", invalidAmount, "salary");
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(newTransfer);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_returnsViolations_whenDescriptionTooLong() {

        TransferRequest newRequest = new TransferRequest("100000000001", "100000000012", BigDecimal.valueOf(250.00), "a".repeat(256));
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(newRequest);
        assertThat(violations).isNotEmpty();
    }
}
