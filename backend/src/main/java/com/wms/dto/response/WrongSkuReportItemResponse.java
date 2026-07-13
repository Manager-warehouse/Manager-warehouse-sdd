package com.wms.dto.response;

import java.math.BigDecimal;

public record WrongSkuReportItemResponse(
    Long id,
    Long transferItemId,
    Long expectedProductId,
    String expectedProductSku,
    String expectedProductName,
    Long actualProductId,
    String actualProductSku,
    String actualProductName,
    BigDecimal affectedQty,
    String reason,
    String photoRef
) {}
