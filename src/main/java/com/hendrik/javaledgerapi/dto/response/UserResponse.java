package com.hendrik.javaledgerapi.dto.response;

import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        @Schema(description = "Unique user identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Registered email address.", example = "jane.doe@example.com")
        String email,

        @Schema(description = "Assigned role. ADMIN is required to create deposits.", example = "USER")
        Role role,

        @Schema(description = "Timestamp the user registered.", example = "2026-08-19T09:15:30")
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
