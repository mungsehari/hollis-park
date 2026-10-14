package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/** The request is well formed but the current state forbids it: a duplicate code, an illegal status change, removing the last admin. Retrying unchanged fails again → CONFLICT. */
public class ConflictException extends AppException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
