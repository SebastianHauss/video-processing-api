package com.sebastianhauss.videoplatform.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class VideoNotFoundException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.VIDEO_NOT_FOUND;

    public VideoNotFoundException(UUID id) {
        super("Video with id '" + id + "' not found");
    }
}
