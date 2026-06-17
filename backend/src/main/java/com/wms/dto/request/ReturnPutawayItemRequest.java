package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnPutawayItemRequest {

    @NotNull(message = "RECEIPT_ITEM_ID_REQUIRED")
    private Long receiptItemId;

    @NotNull(message = "PASSED_LOCATION_ID_REQUIRED")
    private Long passedLocationId; // location_id cho hàng Đạt QC (regular bin)

    @NotNull(message = "FAILED_LOCATION_ID_REQUIRED")
    private Long failedLocationId; // location_id cho hàng Lỗi QC (quarantine bin)
}
