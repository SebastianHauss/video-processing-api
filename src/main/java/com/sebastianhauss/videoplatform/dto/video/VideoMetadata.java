package com.sebastianhauss.videoplatform.dto.video;

public record VideoMetadata(
        String contentType,
        Long durationMillis
) {}
