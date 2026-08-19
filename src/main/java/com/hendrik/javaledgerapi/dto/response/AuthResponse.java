package com.hendrik.javaledgerapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "JWT bearer token. Send it as `Authorization: Bearer <token>` on "
                + "protected requests.", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqYW5lLmRvZUBleGFtcGxlLmNvbSJ9.signature")
        String token,

        @Schema(description = "Always `Bearer`.", example = "Bearer")
        String tokenType
) {
}
