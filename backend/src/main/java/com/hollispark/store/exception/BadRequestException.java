package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/** The request was understood but its content is not acceptable: a missing or invalid field, an unknown product or size, a code that does not exist → BAD_REQUEST. */
public class BadRequestException extends AppException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
