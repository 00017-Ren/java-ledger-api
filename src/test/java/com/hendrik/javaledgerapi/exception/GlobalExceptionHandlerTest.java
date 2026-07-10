package com.hendrik.javaledgerapi.exception;

import com.hendrik.javaledgerapi.dto.response.ErrorResponse;
import com.hendrik.javaledgerapi.model.Account;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();


    @Test
    void handleApiException_returnsErrorResponseWithExceptionStatusAndMessage() {

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/123");

        ResourceNotFoundException exception = new ResourceNotFoundException("Account not found");

        ResponseEntity<ErrorResponse> response =  handler.handleApiException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ErrorResponse body =  response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("Account not found");
        assertThat(body.path()).isEqualTo("/api/v1/123");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void handleMethodArgumentNotValidException_returnsErrorResponseWithExceptionStatusAndMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/123");

        MethodParameter methodParameter = mock(MethodParameter.class);

        FieldError fieldError = new FieldError("objectName", "Email", "Must not be blank");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response =  handler.handleMethodArgumentNotValidException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponse body =  response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Email: Must not be blank");
        assertThat(body.path()).isEqualTo("/api/v1/123");
        assertThat(body.timestamp()).isNotNull();

    }


    @Test
    void handleOptimisticLockingException_returnsErrorResponseWithExceptionStatusAndMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/123");

        ObjectOptimisticLockingFailureException exception = new ObjectOptimisticLockingFailureException(Account.class, "Test Account");

        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailureException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ErrorResponse body =  response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.error()).isEqualTo("Conflict");
        assertThat(body.message()).isNotNull();
        assertThat(body.path()).isEqualTo("/api/v1/123");
        assertThat(body.timestamp()).isNotNull();
    }
}
