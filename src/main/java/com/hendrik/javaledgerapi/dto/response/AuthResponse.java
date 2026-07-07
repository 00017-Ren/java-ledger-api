package com.hendrik.javaledgerapi.dto.response;

public record AuthResponse(
        String token,

        String tokenType
) {
}
