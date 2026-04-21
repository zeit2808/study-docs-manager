package com.studydocs.manager.exception;

import com.studydocs.manager.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Request validation failed",
                request);
        response.setErrors(errors);

        log.warn("Validation error [{} {}]: {}", request.getMethod(), request.getRequestURI(), errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        logByStatus(ex.getStatus(), request, ex, ex.getCode());
        ErrorResponse response = buildErrorResponse(ex.getStatus(), ex.getError(), ex.getMessage(), request);
        response.setCode(ex.getCode());
        response.setField(ex.getField());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        String message = "Missing required parameter: " + ex.getParameterName();
        log.warn("Client error [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                message,
                request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String message = "Invalid value for parameter: " + ex.getName();
        log.warn("Client error [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                message,
                request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Malformed request body [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Malformed JSON request body",
                request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        log.warn("Constraint violation [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Constraint validation failed",
                request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Unhandled runtime exception [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        log.warn("Payload too large [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ErrorResponse response = buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File Too Large",
                "File size exceeds maximum allowed size of 5MB",
                request);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex, HttpServletRequest request) {
        log.error("I/O exception [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Failed to process file: " + ex.getMessage(),
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String error, String message,
            HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse();
        response.setStatus(status.value());
        response.setError(error);
        response.setMessage(message);
        response.setPath(request.getRequestURI());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    private void logByStatus(HttpStatus status, HttpServletRequest request, AppException ex, String code) {
        String message = String.format("App exception [%s %s]: status=%d code=%s message=%s",
                request.getMethod(), request.getRequestURI(), status.value(), code, ex.getMessage());
        if (status.is5xxServerError()) {
            log.error(message, ex);
            return;
        }
        log.warn(message);
    }
}
