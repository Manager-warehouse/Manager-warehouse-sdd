package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record InterWarehouseTransferFinalPutawayItemRequest(
        @NotNull Long transferItemId,
        @NotEmpty List<@Valid InterWarehouseTransferPutawayAllocationRequest> allocations) {}
