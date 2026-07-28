package com.wms.dto.response;

import java.math.BigDecimal;

public record ProductAvailabilityResponse(
        Long productId,
        BigDecimal totalQty,
        BigDecimal reservedQty,
        BigDecimal availableQty) {
}
