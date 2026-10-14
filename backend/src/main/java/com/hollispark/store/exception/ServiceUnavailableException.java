package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/** Something the store needs is not set up yet (e.g. missing store settings) → SERVICE_UNAVAILABLE. */
public class ServiceUnavailableException extends AppException {
    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
