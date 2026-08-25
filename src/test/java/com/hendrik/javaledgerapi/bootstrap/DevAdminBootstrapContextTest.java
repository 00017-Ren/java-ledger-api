package com.hendrik.javaledgerapi.bootstrap;

import com.hendrik.javaledgerapi.config.DevAdminProperties;
import com.hendrik.javaledgerapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DevAdminBootstrapContextTest {

    private static final String TEST_PASSWORD = "ValidTestPassword1";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BootstrapTestConfiguration.class);

    @Test
    void bootstrap_isAbsentOutsideDevProfile_whenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues(
                        "ledger.dev-admin.enabled=true",
                        "ledger.dev-admin.email=admin@bootstrap.test",
                        "ledger.dev-admin.password=" + TEST_PASSWORD)
                .run(context -> assertThat(context).doesNotHaveBean(DevAdminBootstrap.class));
    }

    @Test
    void bootstrap_isAbsentInDevProfile_whenNotExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).doesNotHaveBean(DevAdminBootstrap.class));
    }

    @Test
    void context_failsWithoutLeakingPassword_whenEnabledConfigurationIsInvalid() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "ledger.dev-admin.enabled=true",
                        "ledger.dev-admin.email=not-an-email",
                        "ledger.dev-admin.password=" + TEST_PASSWORD)
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("ledger.dev-admin")
                            .doesNotContain(TEST_PASSWORD);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DevAdminProperties.class)
    static class BootstrapTestConfiguration {

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
