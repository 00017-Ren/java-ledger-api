package com.hendrik.javaledgerapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI ledgerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java Ledger API")
                        .version("v1")
                        .description("A ledger service for user registration and login, "
                                + "account management, and money-movement operations "
                                + "(deposits and transfers), secured with JWT bearer "
                                + "authentication."))
                .tags(List.of(
                        new Tag().name("Authentication").description(
                                "Registration and login. Public, no authentication required."),
                        new Tag().name("Users").description(
                                "Authenticated user profile lookup."),
                        new Tag().name("Accounts").description(
                                "Account creation, listing, and balance lookup."),
                        new Tag().name("Transactions").description(
                                "Deposits, transfers, and transaction history.")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /**
     * {@code @AuthenticationPrincipal} resolves from the security context, not a
     * request parameter, so it must not appear as a documented input.
     */
    @Bean
    public ParameterCustomizer authenticationPrincipalParameterCustomizer() {
        return (parameterModel, methodParameter) ->
                methodParameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        ? null
                        : parameterModel;
    }
}
