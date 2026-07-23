package com.hendrik.javaledgerapi.security;


import com.hendrik.javaledgerapi.model.enums.Role;

import java.util.UUID;

public record JwtUserPrincipal(
        UUID id,
        String email,
        Role role
) {
}
