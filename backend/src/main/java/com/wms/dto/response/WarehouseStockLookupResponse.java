package com.wms.dto.response;

import java.math.BigDecimal;

public record WarehouseStockLookupResponse(
    Long warehouseId,
    String warehouseName,
    BigDecimal availableQty
) {}
