package com.sebastianhauss.videoplatform.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

    public UserNotFoundException(UUID id) {
        super("User with id '" + id + "' not found");
    }

    public UserNotFoundException() {
        super("User not found.");
    }
}
