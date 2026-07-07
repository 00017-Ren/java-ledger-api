package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,

        String email,

        Role role,

        LocalDateTime createdAt
) {
}
