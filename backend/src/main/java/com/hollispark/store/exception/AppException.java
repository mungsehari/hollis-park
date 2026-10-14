package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/**
 * Base of every deliberate failure the API raises. The message is written for the person using the
 * store or the admin panel, so {@link GlobalExceptionHandler} returns it as is. Pick the subclass that
 * says what went wrong; never throw this class or a raw {@code RuntimeException} for a client mistake.
 */
public abstract class AppException extends RuntimeException {
    private final HttpStatus status;

    protected AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected AppException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
