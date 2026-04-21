package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Not Found", message);
    }

    public NotFoundException(String message, String code, String field) {
        super(HttpStatus.NOT_FOUND, "Not Found", message, code, field);
    }
}
