package com.wms.exception;


import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/** DTO phản hồi lỗi chuẩn — chứa code, message, timestamp, path (Spec 001). */
@Builder
@Getter
public class ApiErrorResponse {
    private String code;
    private String message;
    private String error;
    private Map<String, Object> details;
    private OffsetDateTime timestamp;
}
