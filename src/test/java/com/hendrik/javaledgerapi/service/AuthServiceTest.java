package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.dto.request.LoginRequest;
import com.hendrik.javaledgerapi.dto.request.RegisterRequest;
import com.hendrik.javaledgerapi.dto.response.UserResponse;
import com.hendrik.javaledgerapi.exception.DuplicateResourceException;
import com.hendrik.javaledgerapi.exception.InvalidCredentialsException;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.UserRepository;
import com.hendrik.javaledgerapi.security.JwtService;
import com.hendrik.javaledgerapi.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    String rawPassword = "password";
    String hashedPassword = "000xxx000";

    @Test
    void register_returnsUserResponseWithHashedPasswordAndUserRole_whenEmailNotTaken() {
        when(userRepository.existsByEmail("test@domain.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);

        User newUser = new User(
                UUID.randomUUID(),
                "test@domain.com",
                hashedPassword,
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        UserResponse result = authService.register(new RegisterRequest("test@domain.com", rawPassword));

        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(result.email()).isEqualTo("test@domain.com");
        assertThat(result.role()).isEqualTo(Role.USER);
        assertThat(result.createdAt()).isEqualTo(newUser.getCreatedAt());

        assertThat(capturedUser.getPasswordHash()).isEqualTo(hashedPassword);
        assertThat(capturedUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_throwsDuplicateResourceException_whenEmailAlreadyTaken() {
        when(userRepository.existsByEmail("test@domain.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("test@domain.com", rawPassword)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsDuplicateResourceException_whenSaveThrowsDataIntegrityViolationException() {
        when(userRepository.existsByEmail("test@domain.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Email already exists"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("test@domain.com", rawPassword)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void login_returnsAuthResponseWithToken_whenCredentialsValid() {
        UserPrincipal principal = new UserPrincipal(new User(
                UUID.randomUUID(),
                "test@domain.com",
                hashedPassword,
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()));

        var tokenCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        when(authenticationManager.authenticate(tokenCaptor.capture()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null));
        when(jwtService.generateAccessToken(any())).thenReturn("mocked-jwt-token");

        var result = authService.login(new LoginRequest("test@domain.com", rawPassword));
        verify(jwtService).generateAccessToken(principal);

        var capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getPrincipal()).isEqualTo("test@domain.com");
        assertThat(capturedToken.getCredentials()).isEqualTo(rawPassword);
    }

    @Test
    void login_throwsInvalidCredentialsException_whenPasswordWrong() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("test@domain.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_throwsInvalidCredentialsException_whenEmailUnknown() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("unknown@domain.com", rawPassword)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

    }
}
