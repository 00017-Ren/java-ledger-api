package com.hendrik.javaledgerapi.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hendrik.javaledgerapi.dto.request.DepositRequest;
import com.hendrik.javaledgerapi.dto.request.TransferRequest;
import com.hendrik.javaledgerapi.dto.response.TransactionResponse;
import com.hendrik.javaledgerapi.exception.GlobalExceptionHandler;
import com.hendrik.javaledgerapi.exception.ResourceNotFoundException;
import com.hendrik.javaledgerapi.exception.UnauthorizedAccessException;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import com.hendrik.javaledgerapi.security.JwtUserPrincipal;
import com.hendrik.javaledgerapi.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {
    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private UUID userId;
    private UUID transactionId;
    private JsonMapper jsonMapper = new JsonMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        userId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Role role) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "test@domain.com", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );
    }

    @Test
    void post_returns201_whenDepositRequestValid() throws Exception {

        authenticate(Role.ADMIN);

        DepositRequest depositRequest = new DepositRequest(
                "123456789012",
                BigDecimal.TWO,
                "deposit"
        );

        TransactionResponse transactionResponse = new TransactionResponse(
                transactionId,
                null,
                "123456789012",
                BigDecimal.TWO,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "deposit",
                LocalDateTime.now()
        );

        when(transactionService.deposit(Role.ADMIN, depositRequest)).thenReturn(transactionResponse);

        mockMvc.perform(post("/api/v1/transactions/deposit").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/api/v1/transactions/" + transactionId))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    void post_returns403_whenPrincipalNotAdminRole() throws Exception {
        authenticate(Role.USER);

        DepositRequest depositRequest = new DepositRequest(
                "123456789012",
                BigDecimal.TWO,
                "deposit"
        );

        when(transactionService.deposit(Role.USER, depositRequest)).thenThrow(new UnauthorizedAccessException("Not Authorized"));

        mockMvc.perform(post("/api/v1/transactions/deposit").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isForbidden());

        verify(transactionService, times(1)).deposit(Role.USER, depositRequest);
    }

    @Test
    void post_returns400_whenDepositRequestMalformed() throws Exception {
        authenticate(Role.ADMIN);

        DepositRequest depositRequest = new DepositRequest(
                "123456789012",
                BigDecimal.ZERO,
                "deposit"
        );

        mockMvc.perform(post("/api/v1/transactions/deposit").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void post_returns201_whenTransferRequestValid() throws Exception {
        authenticate(Role.ADMIN);

        TransferRequest transferRequest = new TransferRequest(
                "123456789012",
                "123456789013",
                BigDecimal.TEN,
                "transfer"
        );

        TransactionResponse transactionResponse = new TransactionResponse(
                transactionId,
                "123456789012",
                "123456789013",
                BigDecimal.TEN,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                "transfer",
                LocalDateTime.now()
        );

        when(transactionService.transfer(userId, transferRequest)).thenReturn(transactionResponse);

        mockMvc.perform(post("/api/v1/transactions/transfer").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/api/v1/transactions/" + transactionId))
                .andExpect(jsonPath("$.type").value("TRANSFER"));
    }

    @Test
    void post_returns400_whenTransferRequestMalformed() throws Exception {
        authenticate(Role.ADMIN);

        TransferRequest transferRequest = new TransferRequest(
                "123456789012",
                "123456789013",
                BigDecimal.ZERO,
                "transfer"
        );

        mockMvc.perform(post("/api/v1/transactions/transfer").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void post_returns404_whenAccountNotFound() throws Exception {
        authenticate(Role.USER);

        TransferRequest transferRequest = new TransferRequest(
                "123456789012",
                "123456789013",
                BigDecimal.TEN,
                "transfer"
        );

        when(transactionService.transfer(userId, transferRequest)).thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(post("/api/v1/transactions/transfer").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"));

        verify(transactionService, times(1)).transfer(userId, transferRequest);
    }

    @Test
    void get_returns200_whenGetTransactionByIdRequestValid() throws Exception {
        authenticate(Role.USER);

        TransactionResponse transactionResponse = new TransactionResponse(
                transactionId,
                "123456789012",
                "123456789013",
                BigDecimal.TEN,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                "transfer",
                LocalDateTime.now()
        );

        when(transactionService.getTransactionById(transactionId, userId)).thenReturn(transactionResponse);

        mockMvc.perform(get("/api/v1/transactions/" + transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()));
    }
}
