package com.sebastianhauss.videoplatform.dto.user;

import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String username
) {}