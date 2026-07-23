package com.wms.dto.response;

import com.wms.enums.order_fulfillment.ReturnedGoodsQualityResult;
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
    private BigDecimal countedQty;
    private ReturnedGoodsQualityResult qualityResult;
    private String qualityReason;
    private Long destinationLocationId;
    private BigDecimal plannedQty;
    private BigDecimal putawayCompletedQty;
}
