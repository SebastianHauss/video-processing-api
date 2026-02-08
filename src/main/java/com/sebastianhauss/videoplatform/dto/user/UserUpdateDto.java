package com.sebastianhauss.videoplatform.dto.user;

import jakarta.validation.constraints.Email;

public record UserUpdateDto(

        @Email(message = "Invalid email format")
        String email  // nullable - only update if provided

        // Don't allow username changes for simplicity
        // Don't allow password changes here - use separate endpoint
) {
}