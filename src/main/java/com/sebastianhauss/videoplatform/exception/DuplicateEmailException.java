package com.sebastianhauss.videoplatform.exception;

import lombok.Getter;

@Getter
public class DuplicateEmailException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.DUPLICATE_EMAIL;

    public DuplicateEmailException(String email) {
        super("Email " + email + " already exists");
    }
}
