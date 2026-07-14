package com.hendrik.javaledgerapi.service;

import com.hendrik.javaledgerapi.dto.request.RegisterRequest;
import com.hendrik.javaledgerapi.dto.response.UserResponse;
import com.hendrik.javaledgerapi.exception.DuplicateResourceException;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
