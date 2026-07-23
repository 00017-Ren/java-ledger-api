package com.hendrik.javaledgerapi.controller;

import com.hendrik.javaledgerapi.exception.GlobalExceptionHandler;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.UserRepository;
import com.hendrik.javaledgerapi.security.JwtUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void get_returnsCurrentUser_whenUserIdFound() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "test@domain.com", "password", Role.USER,
                LocalDateTime.now(), LocalDateTime.now());
        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(userId, "test@domain.com", Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        jwtUserPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        mockMvc.perform(get("/api/v1/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@domain.com"));

        verify(userRepository).findById(userId);
    }

    @Test
    void get_returns404_whenUserIdNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(userId, "test@domain.com", Role.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        jwtUserPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
