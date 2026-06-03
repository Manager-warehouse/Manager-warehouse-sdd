package com.wms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        String code = e.getMessage();
        HttpStatus status = switch (code) {
            case "INVALID_CREDENTIALS", "ACCOUNT_INACTIVE", "TOKEN_EXPIRED", "TOKEN_INVALID" -> HttpStatus.UNAUTHORIZED;
            case "OTP_INVALID", "OTP_EXPIRED", "VALIDATION_ERROR", "LOCATION_HAS_STOCK", "WAREHOUSE_HAS_STOCK" -> HttpStatus.BAD_REQUEST;
            case "BIN_OVER_CAPACITY" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "DUPLICATE_PLATE_NUMBER", "DUPLICATE_LICENSE_NUMBER", "DUPLICATE_DRIVER_USER", "DUPLICATE_WAREHOUSE_CODE", "DUPLICATE_LOCATION_CODE" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(Map.of("error", code));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("VALIDATION_ERROR");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
