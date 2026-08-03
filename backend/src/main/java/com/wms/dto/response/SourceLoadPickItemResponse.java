package com.wms.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SourceLoadPickItemResponse(
        Long transferItemId,
        Long productId,
        String productSku,
        String productName,
        BigDecimal plannedQty,
        List<SourceLoadPickCandidateResponse> candidates
) {}
