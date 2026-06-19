package com.wms.dto.response;

import java.math.BigDecimal;

public record InventoryAvailabilityResponse(
        Long warehouseId,
        Long productId,
        BigDecimal totalQty,
        BigDecimal reservedQty,
        BigDecimal availableQty) {
}
