package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/** Signed in, but this person may not touch this record → FORBIDDEN. */
public class ForbiddenException extends AppException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
