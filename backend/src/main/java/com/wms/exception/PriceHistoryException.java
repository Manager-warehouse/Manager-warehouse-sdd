package com.wms.exception;

import org.springframework.http.HttpStatus;

/** Typed exception for pricing state/overlap violations. */
public class PriceHistoryException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public PriceHistoryException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static PriceHistoryException alreadyApproved() {
        return new PriceHistoryException(HttpStatus.CONFLICT, "PRICE_ALREADY_APPROVED",
                "Bản giá đã được duyệt, không thể thay đổi.");
    }

    public static PriceHistoryException alreadyCancelled() {
        return new PriceHistoryException(HttpStatus.CONFLICT, "PRICE_ALREADY_CANCELLED",
                "Bản giá đã bị hủy, không thể thay đổi.");
    }

    public static PriceHistoryException overlappingDate() {
        return new PriceHistoryException(HttpStatus.CONFLICT, "OVERLAPPING_EFFECTIVE_DATE",
                "Khoảng ngày trùng với bản giá APPROVED đang tồn tại cho cùng sản phẩm.");
    }

    public static PriceHistoryException invalidDateRange() {
        return new PriceHistoryException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                "effective_date phải nhỏ hơn hoặc bằng end_date.");
    }

    public static PriceHistoryException missingPrice(String productIds) {
        return new PriceHistoryException(HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_PRICE",
                "Không tìm thấy bản giá APPROVED cho sản phẩm: " + productIds);
    }
}
