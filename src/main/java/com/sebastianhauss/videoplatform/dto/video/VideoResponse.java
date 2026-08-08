package com.sebastianhauss.videoplatform.dto.video;

import com.sebastianhauss.videoplatform.domain.video.VideoStatus;
import com.sebastianhauss.videoplatform.domain.video.VideoVisibility;

import java.time.Instant;
import java.util.UUID;

public record VideoResponse(
        UUID id,
        String originalFilename,
        VideoStatus status,
        VideoVisibility visibility,
        long sizeBytes,
        long durationMillis,
        String contentType,
        String thumbnailKey,
        Instant createdAt
) {}
