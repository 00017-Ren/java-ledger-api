package com.hendrik.javaledgerapi.integration;

import com.hendrik.javaledgerapi.dto.request.DepositRequest;
import com.hendrik.javaledgerapi.dto.request.LoginRequest;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.UserRepository;
import com.hendrik.javaledgerapi.security.JwtService;
import com.hendrik.javaledgerapi.security.UserPrincipal;
import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
class SecurityFilterChainIntegrationTest extends PostgresIntegrationTest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void get_returns200_whenHealthRequestedWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void get_returns401_whenActuatorEnvRequestedWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/actuator/env"));
    }

    @Test
    void get_returns404_whenActuatorEnvRequestedWithValidToken() throws Exception {
        User user = testDataFactory.persistUser(Role.USER);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(userPrincipal);

        mockMvc.perform(get("/actuator/env")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_returns401_whenAuthHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Missing or invalid authentication token"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }

    @Test
    void get_returns401_whenAuthTokenInvalid() throws Exception {
        User user = testDataFactory.persistUser(Role.USER);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String tamperedToken = accessToken + "x";

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Missing or invalid authentication token"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }

    @Test
    void get_returns200_whenAuthTokenValid() throws Exception {
        User user = testDataFactory.persistUser(Role.USER);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(userPrincipal);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void post_returns200_whenLoginValid() throws Exception {
        User user = testDataFactory.persistUser(Role.USER);
        String rawPassword = "password123";
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);

        LoginRequest request = new LoginRequest(user.getEmail(), rawPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void post_returns403_whenDepositAsUser() throws Exception {
        User user = testDataFactory.persistUser(Role.USER);
        Account account = testDataFactory.persistAccount(user, BigDecimal.TEN);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(userPrincipal);

        DepositRequest depositRequest = new DepositRequest(
                account.getAccountNumber(),
                BigDecimal.ONE,
                "Deposit"
        );

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Not authorized."))
                .andExpect(jsonPath("$.path").value("/api/v1/transactions/deposit"));
    }

    @Test
    void post_returns201_whenADMINRequestsDeposit() throws Exception {
        User user = testDataFactory.persistUser(Role.ADMIN);
        Account account = testDataFactory.persistAccount(user, BigDecimal.TEN);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(userPrincipal);

        DepositRequest depositRequest = new DepositRequest(
                account.getAccountNumber(),
                BigDecimal.ONE,
                "Deposit"
        );

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.destinationAccountNumber").value(account.getAccountNumber()))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Account reloadedAccount = accountRepository.findByAccountNumber(account.getAccountNumber()).orElseThrow(
                () -> new AssertionError("Expected deposited account to exist.")
        );

        assertThat(reloadedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(11));
    }
}
