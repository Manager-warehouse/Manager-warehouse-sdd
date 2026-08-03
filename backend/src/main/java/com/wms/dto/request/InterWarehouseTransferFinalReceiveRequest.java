package com.wms.dto.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public record InterWarehouseTransferFinalReceiveRequest(
        @Size(max = 1000, message = "REASON_TOO_LONG")
        String discrepancyReason,
        List<@Valid InterWarehouseTransferFinalPutawayItemRequest> putawayItems) {

    public InterWarehouseTransferFinalReceiveRequest(String discrepancyReason) {
        this(discrepancyReason, null);
    }
}
