package com.hendrik.javaledgerapi.bootstrap;

import com.hendrik.javaledgerapi.config.DevAdminProperties;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevAdminBootstrapTest {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "TestPass1";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    private DevAdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        bootstrap = new DevAdminBootstrap(new DevAdminProperties(true, EMAIL, PASSWORD), userRepository,
                passwordEncoder);
    }

    @Test
    void run_createsEncodedAdminWhenEmailIsUnused() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        bootstrap.run(new DefaultApplicationArguments());

        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void run_doesNothingWhenAdminAlreadyExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithRole(Role.ADMIN)));

        bootstrap.run(new DefaultApplicationArguments());

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void run_failsWhenNormalUserAlreadyUsesConfiguredEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithRole(Role.USER)));

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Dev admin email is already assigned to a non-admin user");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void run_acceptsExistingAdminWhenConcurrentInsertWinsRace() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(userWithRole(Role.ADMIN)));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        bootstrap.run(new DefaultApplicationArguments());

        verify(userRepository, times(2)).findByEmail(EMAIL);
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void run_failsWhenConcurrentInsertCreatesNormalUser() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(userWithRole(Role.USER)));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Dev admin email is already assigned to a non-admin user");

        verify(userRepository, times(2)).findByEmail(EMAIL);
    }

    private User userWithRole(Role role) {
        return new User(null, EMAIL, "existing-hash", role, null, null);
    }
}
