package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InterWarehouseTransferPutawayAllocationRequest(
        @NotNull(message = "LOCATION_ID_REQUIRED") Long locationId,
        @NotNull(message = "PUTAWAY_QUANTITY_REQUIRED")
        @Positive(message = "PUTAWAY_QUANTITY_MUST_BE_POSITIVE")
        BigDecimal quantity) {}
