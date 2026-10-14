package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/** Wrong or missing credentials at sign-in → UNAUTHORIZED. */
public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
