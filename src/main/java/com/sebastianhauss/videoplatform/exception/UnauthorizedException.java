package com.sebastianhauss.videoplatform.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

    public UnauthorizedException(String message) {
        super(message);
    }
}
