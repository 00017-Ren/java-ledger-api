package com.hendrik.javaledgerapi.controller;

import com.hendrik.javaledgerapi.dto.response.UserResponse;
import com.hendrik.javaledgerapi.exception.ResourceNotFoundException;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.repository.UserRepository;
import com.hendrik.javaledgerapi.security.JwtUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // TODO: Replace the temporary Principal parameter with JwtUserPrincipal,
    // fix the return type to ResponseEntity<UserResponse>, and finish the
    // /me endpoint wiring in the next session.
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal, Principal principal) {

        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not fond"));
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
