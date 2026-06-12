package com.wms.exception;

import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.exception.ReceiptAlreadyDecidedException;
import com.wms.exception.RtvAlreadyExistsException;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleViolationException ex) {
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        String code = "BUSINESS_RULE_VIOLATION";

        if (msg != null) {
            if (msg.contains("INVENTORY_VERSION_CONFLICT")) {
                status = HttpStatus.CONFLICT;
                code = "INVENTORY_VERSION_CONFLICT";
            } else if (msg.contains("RTV_ALREADY_CONFIRMED")) {
                status = HttpStatus.CONFLICT;
                code = "RTV_ALREADY_CONFIRMED";
            } else if (msg.contains("RTV_QUANTITY_MISMATCH")) {
                code = "RTV_QUANTITY_MISMATCH";
            } else if (msg.contains("INVALID_STATE")) {
                code = "INVALID_STATE";
            } else if (msg.contains("INVALID_LOCATION")) {
                code = "INVALID_LOCATION";
            } else if (msg.contains("BIN_CAPACITY_EXCEEDED")) {
                code = "BIN_CAPACITY_EXCEEDED";
            } else if (msg.contains("INVENTORY_INVARIANT_VIOLATED")) {
                code = "INVENTORY_INVARIANT_VIOLATED";
            }
        }

        return error(status, code, msg, msg, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", "Access denied", null);
    }

    @ExceptionHandler(ForbiddenReceiptWarehouseException.class)
    public ResponseEntity<ApiErrorResponse> handleForbiddenReceiptWarehouse(
            ForbiddenReceiptWarehouseException ex) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN_RECEIPT_WAREHOUSE",
                ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(ReceiptAlreadyDecidedException.class)
    public ResponseEntity<ApiErrorResponse> handleReceiptAlreadyDecided(
            ReceiptAlreadyDecidedException ex) {
        return error(HttpStatus.CONFLICT, "RECEIPT_ALREADY_DECIDED",
                ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(RtvAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleRtvAlreadyExists(
            RtvAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, "RTV_ALREADY_EXISTS",
                ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", errorMsg, errorMsg, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return error(HttpStatus.valueOf(ex.getStatusCode().value()),
                "REQUEST_ERROR", ex.getReason(), ex.getReason(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = "INVALID_ARGUMENT";

        if (msg != null) {
            if (msg.contains("INVALID_CREDENTIALS") || msg.contains("TOKEN_INVALID") || msg.contains("TOKEN_EXPIRED")) {
                status = HttpStatus.UNAUTHORIZED;
                code = "UNAUTHORIZED";
            } else if (msg.contains("DUPLICATE") || msg.contains("ALREADY_EXISTS")) {
                status = HttpStatus.CONFLICT;
                code = "DUPLICATE_RESOURCE";
            } else if (msg.contains("NOT_FOUND")) {
                status = HttpStatus.NOT_FOUND;
                code = "RESOURCE_NOT_FOUND";
            } else if (msg.contains("CAPACITY") || msg.contains("STOCK") || msg.contains("ACTIVE") || msg.contains("TRIP")) {
                status = HttpStatus.UNPROCESSABLE_ENTITY;
                code = "BUSINESS_RULE_VIOLATION";
            }
        }

        return error(status, code, msg, msg, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = "ILLEGAL_STATE";

        if (msg != null) {
            if (msg.contains("ACCOUNT_INACTIVE")) {
                status = HttpStatus.UNAUTHORIZED;
                code = "UNAUTHORIZED";
            } else if (msg.contains("MAIL_SEND_FAILED")) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                code = "MAIL_SEND_FAILED";
            }
        }

        return error(status, code, msg, msg, null);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status,
                                                   String code,
                                                   String message,
                                                   String errorVal,
                                                   Map<String, Object> details) {
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .code(code)
                .message(message)
                .error(errorVal)
                .details(details)
                .timestamp(OffsetDateTime.now())
                .build());
    }
}
