package com.hendrik.javaledgerapi.security;

import com.hendrik.javaledgerapi.model.enums.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    JwtService jwtService;
    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter =  new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testValidToken() throws ServletException, IOException {
        String token = "test-token";
        Claims mockClaims = mock(Claims.class);

        when(mockClaims.get("userId", String.class)).thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(mockClaims.getSubject()).thenReturn("test@domain.com");
        when(mockClaims.get("role", String.class)).thenReturn("USER");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractClaims(token)).thenReturn(mockClaims);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(JwtUserPrincipal.class);

        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();
        assertThat(principal.id()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(principal.email()).isEqualTo("test@domain.com");
        assertThat(principal.role()).isEqualTo(Role.USER);

        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void testMissingAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(any(), any());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertThat(auth).isNull();
    }

    @Test
    void testInvalidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtService.validateToken("some-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(any(), any());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertThat(auth).isNull();
    }
}
