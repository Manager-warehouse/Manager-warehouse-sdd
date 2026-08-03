package com.wms.dto.response;


import com.wms.enums.stock_receiving.ReceiptStatus;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

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
    private List<String> batchCodes;
}
