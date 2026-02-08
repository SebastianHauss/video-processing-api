package com.sebastianhauss.videoplatform.exception;

import lombok.Getter;

@Getter
public class DuplicateUsernameException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.DUPLICATE_USERNAME;

    public DuplicateUsernameException() {
        super("Username already exists");
    }
}
