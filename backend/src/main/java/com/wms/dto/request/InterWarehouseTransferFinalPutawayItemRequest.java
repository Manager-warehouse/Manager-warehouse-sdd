package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record InterWarehouseTransferFinalPutawayItemRequest(
        @NotNull(message = "TRANSFER_ITEM_ID_REQUIRED") Long transferItemId,
        @NotEmpty(message = "PUTAWAY_ALLOCATIONS_REQUIRED")
        List<@Valid InterWarehouseTransferPutawayAllocationRequest> allocations) {}
