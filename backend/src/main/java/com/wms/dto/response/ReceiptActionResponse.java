package com.wms.dto.response;

import com.wms.enums.ReceiptStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Response DTO for receipt approval/reject/return-confirm/putaway-complete actions.
 */
@Getter
@Builder
public class ReceiptActionResponse {

    private Long id;
    private String receiptNumber;
    private ReceiptStatus status;
    private Integer version;
    private OffsetDateTime updatedAt;
    private String message;
}
