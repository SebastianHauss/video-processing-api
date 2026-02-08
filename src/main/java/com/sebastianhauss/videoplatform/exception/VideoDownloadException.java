package com.sebastianhauss.videoplatform.exception;

public class VideoDownloadException extends RuntimeException {
    public VideoDownloadException(String message,  Throwable cause) {
        super(message, cause);
    }
}
