package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateReceiptItemRequest {

    @NotNull
    @JsonProperty("product_id")
    private Long productId;

    @NotNull
    @Min(1)
    @JsonProperty("expected_qty")
    private Integer expectedQty;

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
}
