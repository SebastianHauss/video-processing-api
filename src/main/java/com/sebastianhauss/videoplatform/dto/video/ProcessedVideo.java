package com.sebastianhauss.videoplatform.dto.video;

import com.sebastianhauss.videoplatform.dto.storage.StoredObject;

public record ProcessedVideo(
        StoredObject storedObject,
        long durationMillis,
        String contentType,
        String thumbnailKey
) {}
