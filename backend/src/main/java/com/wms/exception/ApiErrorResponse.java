package com.wms.exception;

import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ApiErrorResponse {
    private String code;
    private String message;
    private Map<String, Object> details;
    private OffsetDateTime timestamp;
}
