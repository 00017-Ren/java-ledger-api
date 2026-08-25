package com.hendrik.javaledgerapi.integration;

import com.hendrik.javaledgerapi.dto.request.DepositRequest;
import com.hendrik.javaledgerapi.dto.request.LoginRequest;
import com.hendrik.javaledgerapi.dto.request.TransferRequest;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.Transaction;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.model.enums.TransactionType;
import com.hendrik.javaledgerapi.repository.AccountRepository;
import com.hendrik.javaledgerapi.repository.TransactionRepository;
import com.hendrik.javaledgerapi.repository.UserRepository;
import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("dev")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "ledger.dev-admin.enabled=true",
        "ledger.dev-admin.email=admin@bootstrap.test",
        "ledger.dev-admin.password=BootstrapTestPassword1"
})
class DevAdminBootstrapFlowIntegrationTest extends PostgresIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@bootstrap.test";
    private static final String ADMIN_PASSWORD = "BootstrapTestPassword1";
    private static final String USER_PASSWORD = "NormalUserPassword1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void bootstrapAdmin_canDeposit_thenNormalUserCanTransfer() throws Exception {
        User admin = userRepository.findByEmail(ADMIN_EMAIL)
                .orElseThrow(() -> new AssertionError("Expected bootstrap admin to exist."));
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPasswordHash()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        User normalUser = testDataFactory.persistUser(Role.USER);
        normalUser.setPasswordHash(passwordEncoder.encode(USER_PASSWORD));
        userRepository.saveAndFlush(normalUser);
        Account sourceAccount = testDataFactory.persistAccount(normalUser, BigDecimal.TEN);
        Account destinationAccount = testDataFactory.persistAccount(normalUser, BigDecimal.ZERO);

        DepositRequest depositRequest = new DepositRequest(
                sourceAccount.getAccountNumber(), BigDecimal.valueOf(25), "Bootstrap flow deposit");
        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));

        assertThat(balanceOf(sourceAccount)).isEqualByComparingTo("35");
        assertThat(transactionRepository.findByAccountId(sourceAccount.getId(), Pageable.unpaged()).getContent())
                .extracting(Transaction::getType)
                .containsExactly(TransactionType.DEPOSIT);

        String normalUserToken = login(normalUser.getEmail(), USER_PASSWORD);
        TransferRequest transferRequest = new TransferRequest(
                sourceAccount.getAccountNumber(), destinationAccount.getAccountNumber(), BigDecimal.valueOf(15),
                "Bootstrap flow transfer");
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + normalUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("TRANSFER"));

        assertThat(balanceOf(sourceAccount)).isEqualByComparingTo("20");
        assertThat(balanceOf(destinationAccount)).isEqualByComparingTo("15");
        assertThat(transactionRepository.findByAccountId(sourceAccount.getId(), Pageable.unpaged()).getContent())
                .extracting(Transaction::getType)
                .containsExactlyInAnyOrder(TransactionType.DEPOSIT, TransactionType.TRANSFER);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", sourceAccount.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + normalUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }

    private BigDecimal balanceOf(Account account) {
        return accountRepository.findByAccountNumber(account.getAccountNumber())
                .orElseThrow(() -> new AssertionError("Expected account to exist."))
                .getBalance();
    }
}
