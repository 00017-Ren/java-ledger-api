package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.dto.request.LoginRequest;
import com.hendrik.javaledgerapi.dto.request.RegisterRequest;
import com.hendrik.javaledgerapi.dto.response.AuthResponse;
import com.hendrik.javaledgerapi.dto.response.UserResponse;
import com.hendrik.javaledgerapi.exception.DuplicateResourceException;
import com.hendrik.javaledgerapi.exception.InvalidCredentialsException;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.UserRepository;
import com.hendrik.javaledgerapi.security.JwtService;
import com.hendrik.javaledgerapi.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        User newUser = new User(null, request.email(), hashedPassword, Role.USER, null, null);
        try {
            User savedUser = userRepository.save(newUser);
            return UserResponse.from(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtService.generateAccessToken(userPrincipal);

            return new AuthResponse(token, "Bearer");

        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }
    }
}
