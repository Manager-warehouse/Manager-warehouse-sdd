package com.wms.dto.request;

import com.wms.enums.order_fulfillment.ReturnedGoodsQualityResult;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnedGoodsCountQcItemRequest {

    @NotNull
    private Long doItemId;

    @NotNull
    private Long productId;

    @NotNull
    private Long batchId;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal countedQty;

    @NotNull
    private ReturnedGoodsQualityResult qualityResult;

    private String qualityReason;
}
