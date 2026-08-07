package com.sebastianhauss.videoplatform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base for all application errors. Carries the HTTP status the API should
 * respond with, so the {@link GlobalExceptionHandler} needs a single handler.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
