package com.wms.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

public record TransferRequestItemResponse(
    Long id,
    Long productId,
    String productSku,
    String productName,
    String productUnit,
    BigDecimal requestedQty
) {}
