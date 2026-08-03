package com.wms.dto.response;

import java.math.BigDecimal;

public record SourceLoadPickCandidateResponse(
        Long inventoryId,
        Long locationId,
        String locationCode,
        Long batchId,
        String batchCode,
        BigDecimal availableQty
) {}
