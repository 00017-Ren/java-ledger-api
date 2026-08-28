package com.hendrik.javaledgerapi.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    private static final String TEST_SECRET = "test-secret-key-at-least-32chars";


    @TempDir
    Path tempDir;

    @EnableConfigurationProperties(JwtProperties.class)
    static class TestConfig {
    }

    @Test
    void context_failsToStart_whenSecretIsBlank() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=",
                        "jwt.access-token-expiration-ms=900000"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .hasStackTraceContaining("secret");
                });
    }

    @Test
    void context_failsToStart_whenSecretIsMissing() {
        contextRunner
                .withPropertyValues(
                        "jwt.access-token-expiration-ms=900000"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .hasStackTraceContaining("secret");
                });
    }

    @Test
    void context_loadsSuccessfully_withConfigTreePath() throws IOException {
        Files.writeString(tempDir.resolve("jwt.secret"), TEST_SECRET);

        contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues(
                "spring.config.import=optional:configtree:" + tempDir + "/", "jwt.access-token-expiration-ms=900000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).secret()).isEqualTo(TEST_SECRET);
                });
    }
}
