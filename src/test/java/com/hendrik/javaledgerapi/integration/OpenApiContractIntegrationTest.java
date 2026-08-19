package com.hendrik.javaledgerapi.integration;

import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OpenApiContractIntegrationTest extends PostgresIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper jsonMapper;

    private JsonNode apiDocs;

    @BeforeEach
    void fetchApiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        apiDocs = jsonMapper.readTree(body);
    }

    @Test
    void getApiDocs_returnsPublicJson_whenCalled() {
        assertThat(apiDocs.path("openapi").asText()).startsWith("3.");
    }

    @Test
    void getApiDocs_returnsAllCurrentOperations_whenCalled() {
        assertThat(hasOperation("/api/v1/auth/register", "post")).isTrue();
        assertThat(hasOperation("/api/v1/auth/login", "post")).isTrue();
        assertThat(hasOperation("/api/v1/users/me", "get")).isTrue();
        assertThat(hasOperation("/api/v1/accounts", "get")).isTrue();
        assertThat(hasOperation("/api/v1/accounts", "post")).isTrue();
        assertThat(hasOperation("/api/v1/accounts/{id}", "get")).isTrue();
        assertThat(hasOperation("/api/v1/accounts/{id}/balance", "get")).isTrue();
        assertThat(hasOperation("/api/v1/accounts/{id}/transactions", "get")).isTrue();
        assertThat(hasOperation("/api/v1/transactions/deposit", "post")).isTrue();
        assertThat(hasOperation("/api/v1/transactions/transfer", "post")).isTrue();
        assertThat(hasOperation("/api/v1/transactions/{id}", "get")).isTrue();
    }

    @Test
    void getApiDocs_returnsHttpBearerJwtScheme_whenCalled() {
        JsonNode scheme = apiDocs.path("components").path("securitySchemes").path("bearerAuth");

        assertThat(scheme.path("type").asText()).isEqualTo("http");
        assertThat(scheme.path("scheme").asText()).isEqualTo("bearer");
        assertThat(scheme.path("bearerFormat").asText()).isEqualTo("JWT");
    }

    @Test
    void getApiDocs_returnsNoSecurityRequirement_whenOperationIsPublic() {
        assertThat(hasSecurityRequirement("/api/v1/auth/register", "post")).isFalse();
        assertThat(hasSecurityRequirement("/api/v1/auth/login", "post")).isFalse();
    }

    @Test
    void getApiDocs_returnsSecurityRequirement_whenOperationIsProtected() {
        assertThat(hasSecurityRequirement("/api/v1/users/me", "get")).isTrue();
        assertThat(hasSecurityRequirement("/api/v1/accounts", "get")).isTrue();
        assertThat(hasSecurityRequirement("/api/v1/accounts", "post")).isTrue();
        assertThat(hasSecurityRequirement("/api/v1/accounts/{id}", "get")).isTrue();
        assertThat(hasSecurityRequirement("/api/v1/accounts/{id}/balance", "get")).isTrue();
        assertThat(hasSecurityRequirement("/api/v1/accounts/{id}/transactions", "get")).isTrue();
        assertThat(hasSecurityRequirement("/api/v1/transactions/deposit", "post")).isTrue();
        assertThat(hasSecurityRequirement("/api/v1/transactions/transfer", "post")).isTrue();
        assertThat(hasSecurityRequirement("/api/v1/transactions/{id}", "get")).isTrue();
    }

    @Test
    void getApiDocs_returnsPagingParameterDefaults_whenOperationIsTransactionHistory() {
        JsonNode parameters = operation("/api/v1/accounts/{id}/transactions", "get").path("parameters");

        JsonNode page = findParameter(parameters, "page");
        JsonNode size = findParameter(parameters, "size");
        JsonNode sort = findParameter(parameters, "sort");

        assertThat(page.path("schema").path("default").asText()).isEqualTo("0");
        assertThat(size.path("schema").path("default").asText()).isEqualTo("20");

        JsonNode sortDefault = sort.path("schema").path("default");
        assertThat(sortDefault.get(0).asText()).isEqualTo("createdAt,DESC");
        assertThat(sortDefault.get(1).asText()).isEqualTo("id,DESC");
    }

    @Test
    void getApiDocs_referencesErrorResponseSchema_whenResponseIsAnError() {
        assertThat(errorSchemaRef("/api/v1/auth/register", "post", "400")).isEqualTo("#/components/schemas/ErrorResponse");
        assertThat(errorSchemaRef("/api/v1/auth/login", "post", "401")).isEqualTo("#/components/schemas/ErrorResponse");
        assertThat(errorSchemaRef("/api/v1/transactions/deposit", "post", "403")).isEqualTo("#/components/schemas/ErrorResponse");
        assertThat(errorSchemaRef("/api/v1/accounts/{id}", "get", "404")).isEqualTo("#/components/schemas/ErrorResponse");
        assertThat(errorSchemaRef("/api/v1/transactions/deposit", "post", "409")).isEqualTo("#/components/schemas/ErrorResponse");
        assertThat(errorSchemaRef("/api/v1/transactions/transfer", "post", "422")).isEqualTo("#/components/schemas/ErrorResponse");
    }

    private JsonNode operation(String path, String method) {
        return apiDocs.path("paths").path(path).path(method);
    }

    private boolean hasOperation(String path, String method) {
        return !operation(path, method).isMissingNode();
    }

    private boolean hasSecurityRequirement(String path, String method) {
        JsonNode security = operation(path, method).path("security");
        return security.isArray() && !security.isEmpty();
    }

    private JsonNode findParameter(JsonNode parameters, String name) {
        for (JsonNode parameter : parameters) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("Expected a '" + name + "' parameter but found none.");
    }

    private String errorSchemaRef(String path, String method, String status) {
        return operation(path, method)
                .path("responses").path(status)
                .path("content").path("*/*")
                .path("schema").path("$ref").asText();
    }
}
