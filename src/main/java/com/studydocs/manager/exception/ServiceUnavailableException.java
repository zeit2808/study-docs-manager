package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends AppException {

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", message);
    }

    public ServiceUnavailableException(String message, String code, String field) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", message, code, field);
    }
}
