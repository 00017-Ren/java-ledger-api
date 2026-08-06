package com.hendrik.javaledgerapi.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hendrik.javaledgerapi.dto.request.CreateAccountRequest;
import com.hendrik.javaledgerapi.dto.response.AccountResponse;
import com.hendrik.javaledgerapi.dto.response.BalanceResponse;
import com.hendrik.javaledgerapi.dto.response.TransactionResponse;
import com.hendrik.javaledgerapi.exception.GlobalExceptionHandler;
import com.hendrik.javaledgerapi.exception.ResourceNotFoundException;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.model.enums.TransactionStatus;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import com.hendrik.javaledgerapi.security.JwtUserPrincipal;
import com.hendrik.javaledgerapi.service.AccountService;
import com.hendrik.javaledgerapi.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {
    @Mock
    private AccountService accountService;
    @Mock
    private TransactionService transactionService;
    @InjectMocks
    private AccountController accountController;

    private AccountResponse accountResponse;
    private BalanceResponse balanceResponse;
    private JsonMapper jsonMapper = new JsonMapper();
    private MockMvc mockMvc;
    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .build();

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser() {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "test@domain.com", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @Test
    void post_returns201_whenUserIsValid() throws Exception {
        User testUser = new User(
                userId,
                "test@domain.com",
                "hashedPassword",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        authenticateUser();

        Account mockAccount = new Account(
                accountId,
                testUser,
                "123456789012",
                BigDecimal.ZERO,
                "ZAR",
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(accountService.createAccount(userId, "ZAR")).thenReturn(mockAccount);

        CreateAccountRequest request = new CreateAccountRequest("ZAR");

        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void get_returns200WithAccount_whenUserIsValid() throws Exception {
        User testUser = new User(
                userId,
                "test@domain.com",
                "hashedPassword",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        authenticateUser();

        Account mockAccount = new Account(
                accountId,
                testUser,
                "123456789012",
                BigDecimal.ZERO,
                "ZAR",
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        accountResponse = AccountResponse.from(mockAccount);

        when(accountService.listUserAccounts(userId)).thenReturn(List.of(accountResponse));

        mockMvc.perform(get("/api/v1/accounts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("123456789012"));
    }

    @Test
    void get_returns200WithAccountResponse_whenPathVariableIsValid() throws Exception {
        User testUser = new User(
                userId,
                "test@domain.com",
                "hashedPassword",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        authenticateUser();

        Account mockAccount = new Account(
                accountId,
                testUser,
                "123456789012",
                BigDecimal.ZERO,
                "ZAR",
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        accountResponse = AccountResponse.from(mockAccount);

        when(accountService.getAccountById(mockAccount.getId(), userId)).thenReturn(accountResponse);

        mockMvc.perform(get("/api/v1/accounts/" + accountId))
                .andExpect(status().isOk());
    }

    @Test
    void get_returns200WithBalanceResponse_whenPathVariableValid() throws Exception {
        User testUser = new User(
                userId,
                "test@domain.com",
                "hashedPassword",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        authenticateUser();

        Account mockAccount = new Account(
                accountId,
                testUser,
                "123456789012",
                BigDecimal.ZERO,
                "ZAR",
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        balanceResponse = BalanceResponse.from(mockAccount);

        when(accountService.getAccountBalance(accountId, userId)).thenReturn(balanceResponse);

        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/balance"))
                .andExpect(status().isOk());
    }

    @Test
    void get_returns404_whenAccountDoesntExist() throws Exception {
        authenticateUser();

        when((accountService.getAccountById(accountId, userId)))
                .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/v1/accounts/" + accountId))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_returns400_whenUserIsNotValid() throws Exception {
        authenticateUser();

        String malformedUUID = "malformedUUID";

        mockMvc.perform(get("/api/v1/accounts/" + malformedUUID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid id: " + malformedUUID));
    }

    @Test
    void get_returnsTransactionResponseAsPageable_whenUserIsValid() throws Exception {
        authenticateUser();

        Pageable pageable = PageRequest.of(1, 5, Sort.by("amount").ascending());

        Page<TransactionResponse> transactionPage = new PageImpl<>(
                List.of(new TransactionResponse(
                        UUID.randomUUID(),
                        "123456789012",
                        "123456789013",
                        BigDecimal.TEN,
                        TransactionType.TRANSFER,
                        TransactionStatus.COMPLETED,
                        "test history",
                        LocalDateTime.now()
                )),
                pageable,
                1);

        when(transactionService.getAccountHistory(eq(accountId), eq(userId), any(Pageable.class))).thenReturn(transactionPage);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);


        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/transactions?page=1&size=5&sort=amount,asc"))
                .andExpect(status().isOk());

        verify(transactionService).getAccountHistory(eq(accountId), eq(userId), captor.capture());
        Pageable capturedPageable = captor.getValue();

        assertThat(capturedPageable.getPageNumber()).isEqualTo(1);
        assertThat(capturedPageable.getPageSize()).isEqualTo(5);
        assertThat(capturedPageable.getSort().getOrderFor("amount")
                .getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
