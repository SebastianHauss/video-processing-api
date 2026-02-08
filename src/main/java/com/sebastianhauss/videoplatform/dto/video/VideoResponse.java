package com.sebastianhauss.videoplatform.dto.video;

import com.sebastianhauss.videoplatform.domain.video.VideoStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

public record VideoResponse(
        UUID id,
        String originalFilename,
        VideoStatus status,
        Long sizeBytes,
        Long durationMillis,
        String contentType,
        Instant createdAt
) {}
