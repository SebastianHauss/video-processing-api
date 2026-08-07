package com.sebastianhauss.videoplatform.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {
    public StorageException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
