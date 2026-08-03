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
    private BigDecimal shortageQty;
    private String shortageReason;
    private Long destinationLocationId;
    private Long failedDestinationLocationId;
    private BigDecimal plannedQty;
    private BigDecimal failedPlannedQty;
    private BigDecimal putawayCompletedQty;
    private BigDecimal failedPutawayCompletedQty;
}
