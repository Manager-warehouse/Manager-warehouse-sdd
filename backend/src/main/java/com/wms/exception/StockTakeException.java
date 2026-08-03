package com.wms.exception;


import org.springframework.http.HttpStatus;

/**
 * Exception nghiệp vụ cho kiểm kê — mang error code và HTTP status cụ thể.
 * Các code thường gặp: OVERLAPPING_STOCKTAKE, INVALID_STATE, INCOMPLETE_COUNT,
 * APPROVAL_LEVEL_MISMATCH, STOCK_TAKE_ALREADY_APPROVED, STOCK_TAKE_NOT_CANCELLABLE,
 * FORBIDDEN_WAREHOUSE, LOCATION_LOCKED, DUPLICATE_ITEM, INVALID_COUNT_QTY,
 * EMPLOYEE_FAULT_REASON_REQUIRED, VARIANCE_REASON_REQUIRED, EMPTY_STOCKTAKE.
 * Xử lý bởi: GlobalExceptionHandler → trả JSON lỗi cho frontend.
 */
public class StockTakeException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public StockTakeException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
