package com.wms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnQcItemRequest {
    @NotNull(message = "RECEIPT_ITEM_REQUIRED")
    private Long receiptItemId;

    @NotNull(message = "ACTUAL_QTY_REQUIRED")
    @Min(value = 0, message = "ACTUAL_QTY_MIN_0")
    private Integer actualQty;

    @NotNull(message = "PASSED_QTY_REQUIRED")
    @Min(value = 0, message = "PASSED_QTY_MIN_0")
    private Integer passedQty;

    @NotNull(message = "FAILED_QTY_REQUIRED")
    @Min(value = 0, message = "FAILED_QTY_MIN_0")
    private Integer failedQty;

    @NotNull(message = "PASSED_LOCATION_REQUIRED")
    private Long passedLocationId;

    @NotNull(message = "QUARANTINE_LOCATION_REQUIRED")
    private Long quarantineLocationId;
}
