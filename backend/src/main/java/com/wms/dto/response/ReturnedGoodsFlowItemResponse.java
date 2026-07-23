package com.wms.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReturnedGoodsFlowItemResponse {
    private Long doItemId;
    private Long productId;
    private Long batchId;
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private BigDecimal qualityPassQty;
    private BigDecimal qualityFailQty;
    private String qualityFailureReason;
    private Long destinationLocationId;
    private BigDecimal plannedQty;
    private BigDecimal putawayCompletedQty;
}
