package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
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
}
