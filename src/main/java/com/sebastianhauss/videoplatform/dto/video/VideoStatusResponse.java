package com.sebastianhauss.videoplatform.dto.video;

import com.sebastianhauss.videoplatform.domain.video.VideoStatus;

import java.util.UUID;

/** Minimal projection for polling a video's processing state. */
public record VideoStatusResponse(
        UUID id,
        VideoStatus status
) {
}
