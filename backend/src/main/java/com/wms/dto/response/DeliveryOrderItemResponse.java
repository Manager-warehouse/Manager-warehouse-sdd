package com.wms.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeliveryOrderItemResponse {
    private Long id;
    private Long productId;
    private Long batchId;
    private Long locationId;
    private BigDecimal requestedQty;
    private BigDecimal reservedQty;
    private BigDecimal issuedQty;
    private BigDecimal unitPrice;
}

