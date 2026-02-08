package com.sebastianhauss.videoplatform.dto.storage;

public record StoredObject(
        String bucket,
        String objectKey
) {}
