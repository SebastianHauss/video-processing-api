package com.sebastianhauss.videoplatform.dto.video;

import com.sebastianhauss.videoplatform.domain.video.VideoStatus;

import java.time.Instant;
import java.util.UUID;

public record VideoResponse(
        UUID id,
        String originalFilename,
        VideoStatus status,
        long sizeBytes,
        long durationMillis,
        String contentType,
        String thumbnailKey,
        Instant createdAt
) {}
