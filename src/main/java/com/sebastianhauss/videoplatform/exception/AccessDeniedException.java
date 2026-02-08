package com.sebastianhauss.videoplatform.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException() {
        super("You are not allowed to access this resource");
    }
}
