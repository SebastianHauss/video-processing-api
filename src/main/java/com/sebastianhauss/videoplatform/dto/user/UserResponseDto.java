package com.sebastianhauss.videoplatform.dto.user;

import com.sebastianhauss.videoplatform.domain.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean enabled,
        Instant createdAt
) {
}