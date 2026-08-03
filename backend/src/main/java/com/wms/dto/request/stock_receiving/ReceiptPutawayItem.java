package com.wms.dto.request.stock_receiving;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Nested DTO mapping an individual receipt item to its selected target location.
 */
@Getter
@Setter
public class ReceiptPutawayItem {

    @NotNull(message = "RECEIPT_ITEM_ID_REQUIRED")
    private Long receiptItemId;

    @NotNull(message = "LOCATION_ID_REQUIRED")
    private Long locationId;

    @NotNull(message = "PUTAWAY_QTY_REQUIRED")
    @Positive(message = "PUTAWAY_QTY_INVALID")
    private Integer quantity;

    @Size(max = 100, message = "EXPECTED_BATCH_CODE_TOO_LONG")
    private String expectedBatchCode;
}
