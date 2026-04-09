package com.populace.common.exception;

import com.populace.common.dto.ErrorResponse;
import com.populace.compensation.exception.CompensationValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        List<ErrorResponse.FieldError> details = ex.getFieldErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(fe.field(), fe.message()))
            .toList();
        ErrorResponse response = ErrorResponse.withDetails(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            ex.getMessage(),
            request.getRequestURI(),
            details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(CompensationValidationException.class)
    public ResponseEntity<ErrorResponse> handleCompensationValidation(
            CompensationValidationException ex, HttpServletRequest request) {
        log.warn("Compensation validation error: {}", ex.getMessage());
        List<ErrorResponse.FieldError> details = ex.getErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(fe.field(), fe.message()))
            .toList();
        ErrorResponse response = ErrorResponse.withDetails(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            ex.getMessage(),
            request.getRequestURI(),
            details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "You do not have permission to perform this action",
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockException ex, HttpServletRequest request) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        List<ErrorResponse.FieldError> details = List.of(
            new ErrorResponse.FieldError("version", ex.getMessage())
        );
        ErrorResponse response = ErrorResponse.withDetails(
            HttpStatus.CONFLICT.value(),
            ex.getCode(),
            ex.getMessage(),
            request.getRequestURI(),
            details);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        BindingResult result = ex.getBindingResult();
        List<ErrorResponse.FieldError> details = result.getFieldErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        ErrorResponse response = ErrorResponse.withDetails(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "Request validation failed",
            request.getRequestURI(),
            details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
        if (ex.getCause() != null) {
            message += " - Caused by: " + ex.getCause().getMessage();
        }
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            message,
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
