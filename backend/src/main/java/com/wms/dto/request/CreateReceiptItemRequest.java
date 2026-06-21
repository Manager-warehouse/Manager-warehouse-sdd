package com.wms.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.config.jackson.StrictIntegerDeserializer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateReceiptItemRequest {

    @NotNull
    @JsonProperty("product_id")
    private Long productId;

    @NotNull
    @Min(1)
    @JsonProperty("expected_qty")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer expectedQty;

    @JsonProperty("unit_cost")
    private BigDecimal unitCost;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getExpectedQty() {
        return expectedQty;
    }

    public void setExpectedQty(Integer expectedQty) {
        this.expectedQty = expectedQty;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }
}
