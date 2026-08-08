package com.sebastianhauss.videoplatform.dto.video;

/**
 * Result of processing a stored video: the metadata extracted and the key of
 * the generated thumbnail (null if thumbnail generation failed).
 */
public record ProcessedVideo(
        long durationMillis,
        String contentType,
        String thumbnailKey
) {}
