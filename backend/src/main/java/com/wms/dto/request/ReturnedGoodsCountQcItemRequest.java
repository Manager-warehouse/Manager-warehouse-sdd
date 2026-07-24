package com.wms.dto.request;

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
    private BigDecimal actualQty;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal qualityPassQty;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal qualityFailQty;

    private String qualityFailureReason;
}
