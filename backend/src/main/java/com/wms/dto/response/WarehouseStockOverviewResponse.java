package com.wms.dto.response;

import java.math.BigDecimal;

public record WarehouseStockOverviewResponse(
        Long warehouseId,
        BigDecimal availableQty,
        long todayReceiptCount,
        long todayDeliveryOrderCount,
        long activeLowStockCount) {
}
