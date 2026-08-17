package com.hendrik.javaledgerapi.integration;

import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.security.JwtService;
import com.hendrik.javaledgerapi.security.UserPrincipal;
import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PaginationIntegrationTest extends PostgresIntegrationTest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Test
    void get_returnsCappedPage_whenRequestedSizeExceedsMaximum() throws Exception {
        User user = testDataFactory.persistUser(Role.USER);
        Account account = testDataFactory.persistAccount(user, BigDecimal.TEN);

        for (int x = 0; x < 101; x++) {
            testDataFactory.persistCompletedDepositTransaction(account, BigDecimal.TEN);
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);

        mockMvc.perform(get("/api/v1/accounts/" + account.getId() + "/transactions?size=101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateAccessToken(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(100))
                .andExpect(jsonPath("$.page.size").value(100))
                .andExpect(jsonPath("$.page.totalElements").value(101))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void get_returnsDefaultPage_whenPaginationParametersAreAbsent() throws Exception {
        User user = testDataFactory.persistUser(Role.USER);
        Account account = testDataFactory.persistAccount(user, BigDecimal.TEN);

        for (int x = 0; x < 21; x++) {
            testDataFactory.persistCompletedDepositTransaction(account, BigDecimal.TEN);
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);

        mockMvc.perform(get("/api/v1/accounts/" + account.getId() + "/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateAccessToken(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(21))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.page.number").value(0));
    }
}
