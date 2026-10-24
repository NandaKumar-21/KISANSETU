package com.kisansetu.common.exception;

import com.kisansetu.common.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, jakarta.servlet.http.HttpServletRequest request) {
        return build(ex.getStatus(), HttpStatus.valueOf(ex.getStatus()).getReasonPhrase(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return build(400, "Bad Request", "Validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return build(400, "Bad Request", "Validation failed", request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, jakarta.servlet.http.HttpServletRequest request) {
        return build(400, "Bad Request", "Invalid parameter: " + ex.getName(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, jakarta.servlet.http.HttpServletRequest request) {
        return build(403, "Forbidden", "You do not have permission to perform this action", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, jakarta.servlet.http.HttpServletRequest request) {
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Data integrity violation: {}", message);
        if (message != null && message.contains("duplicate key")) {
            return build(409, "Conflict", "This record already exists", request, null);
        }
        if (message != null && message.contains("violates check constraint")) {
            return build(400, "Bad Request", "Value is outside allowed range", request, null);
        }
        return build(400, "Bad Request", "Invalid data operation", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, jakarta.servlet.http.HttpServletRequest request) {
        return build(404, "Not Found", "Resource not found", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(500, "Internal Server Error", "Something went wrong. Please try again later.", request, null);
    }

    private ResponseEntity<ErrorResponse> build(int status, String error, String message,
                                                jakarta.servlet.http.HttpServletRequest request,
                                                Map<String, String> validationErrors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status, error, message, request.getRequestURI(), validationErrors);
        return ResponseEntity.status(status).body(body);
    }
}