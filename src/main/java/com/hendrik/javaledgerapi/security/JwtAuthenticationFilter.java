package com.hendrik.javaledgerapi.security;

import com.hendrik.javaledgerapi.model.enums.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService  jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.extractClaims(token);

        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(
                UUID.fromString(claims.get("userId", String.class)),
                claims.getSubject(),
                Role.valueOf(claims.get("role", String.class))
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                jwtUserPrincipal,
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + jwtUserPrincipal.role().name())));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
