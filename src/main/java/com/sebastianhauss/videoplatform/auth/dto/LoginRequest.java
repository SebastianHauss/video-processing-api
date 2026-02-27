package com.sebastianhauss.videoplatform.auth.dto;

public record LoginRequest(
        String username,
        String password
) {}