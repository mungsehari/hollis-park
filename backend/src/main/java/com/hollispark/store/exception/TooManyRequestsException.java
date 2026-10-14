package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/** Too many attempts in a short time (sign-in lockout) → TOO_MANY_REQUESTS. */
public class TooManyRequestsException extends AppException {
    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
