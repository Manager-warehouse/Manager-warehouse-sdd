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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
            } else if (msg.contains("LOCATION_LOCKED")) {
                code = "LOCATION_LOCKED";
            } else if (msg.contains("ACCOUNTING_PERIOD_CLOSED")) {
                code = "ACCOUNTING_PERIOD_CLOSED";
            }
        }

        return error(status, code, msg, msg, null);
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ApiErrorResponse> handleUnprocessable(UnprocessableEntityException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", ex.getMessage(), null, null);
    }

    @ExceptionHandler(ReceiptCountException.class)
    public ResponseEntity<ApiErrorResponse> handleReceiptCount(ReceiptCountException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), null, null);
    }

    @ExceptionHandler(OutboundDeliveryException.class)
    public ResponseEntity<ApiErrorResponse> handleOutboundDelivery(OutboundDeliveryException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), null, ex.getDetails());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticConflict(ObjectOptimisticLockingFailureException ex) {
        return error(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was changed by another transaction; reload and retry", null, null);
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

    @ExceptionHandler(StockTakeException.class)
    public ResponseEntity<ApiErrorResponse> handleStockTake(StockTakeException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(PriceHistoryException.class)
    public ResponseEntity<ApiErrorResponse> handlePriceHistory(PriceHistoryException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getMessage(), null);
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "Invalid request body", null, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason();
        return error(HttpStatus.valueOf(ex.getStatusCode().value()),
                reason == null ? "REQUEST_ERROR" : reason, reason, reason, null);
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

    private String translateMessage(String msg) {
        if (msg == null) return null;
        switch (msg) {
            case "TRIP_SCHEDULE_INVALID": return "Lịch trình chuyến đi không hợp lệ (thời gian kết thúc phải sau thời gian bắt đầu).";
            case "TRIP_START_IN_PAST": return "Thời gian bắt đầu chuyến đi không được ở quá khứ.";
            case "TRIP_END_IN_PAST": return "Thời gian hạn giao hàng không được ở quá khứ.";
            case "TRIP_RESOURCE_OVERLAP": return "Tài xế hoặc phương tiện đã được gán cho một chuyến đi khác trùng thời gian.";
            case "VEHICLE_NOT_AVAILABLE": return "Phương tiện vận tải hiện không khả dụng.";
            case "DRIVER_NOT_AVAILABLE": return "Tài xế hiện không khả dụng.";
            case "DUPLICATE_EXTERNAL_INSTRUCTION": return "Mã chỉ thị điều chuyển này đã tồn tại trên hệ thống.";
            case "TRANSFER_ALREADY_HAS_TRIP": return "Phiếu điều chuyển đã được gán chuyến xe trước đó.";
            case "OVER_RECEIPT_BLOCKED": return "Số lượng thực nhận không được lớn hơn số lượng đã gửi đi.";
            case "QC_TOTAL_MUST_MATCH_CONFIRMED_QTY": return "Tổng số lượng QC đạt và QC lỗi phải bằng số lượng thực nhận.";
            case "QC_FAILURE_REASON_REQUIRED": return "Yêu cầu nhập lý do lỗi khi có số lượng QC không đạt.";
            case "DESTINATION_LOCATION_REQUIRED": return "Yêu cầu chọn vị trí lưu trữ (Bin) cho hàng đạt QC.";
            case "UNSHIP_REQUIRED_BEFORE_CANCEL": return "Cần hủy xuất hàng (Unship) trước khi hủy phiếu điều chuyển.";
            case "TRANSFER_TRIP_REQUIRED": return "Chuyến xe chưa được gán hoặc chưa sẵn sàng khởi hành.";
            case "ASSIGNED_DRIVER_REQUIRED": return "Chỉ tài xế được chỉ định mới có quyền xác nhận khởi hành.";
            case "QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE": return "Hàng đạt QC không thể xếp vào khu vực cách ly (Quarantine). Vui lòng chọn bin lưu trữ thông thường.";
            case "QUARANTINE_LOCATION_NOT_CONFIGURED": return "Kho đích chưa có khu vực cách ly (Quarantine). Cần thêm ít nhất một Bin Quarantine trước khi duyệt QC lỗi.";
            case "WAREHOUSE_REQUIRED": return "Yêu cầu gán kho hoạt động cho tài khoản.";
            case "MULTIPLE_WAREHOUSES_NOT_ALLOWED": return "Chỉ được phép gán tối đa 1 kho cho vai trò này.";
            case "WAREHOUSE_SCOPE_FORBIDDEN": return "Bạn không có quyền truy cập phạm vi kho này.";
            default: return msg;
        }

    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status,
                                                   String code,
                                                   String message,
                                                   String errorVal,
                                                   Map<String, Object> details) {
        String translatedMsg = translateMessage(message);
        String translatedErr = translateMessage(errorVal);
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .code(code)
                .message(translatedMsg)
                .error(translatedErr)
                .details(details)
                .timestamp(OffsetDateTime.now())
                .build());
    }

}
