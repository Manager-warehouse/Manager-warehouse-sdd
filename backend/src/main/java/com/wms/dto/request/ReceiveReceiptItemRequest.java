package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wms.config.jackson.StrictIntegerDeserializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ReceiveReceiptItemRequest {

    @NotNull
    @JsonProperty("receipt_item_id")
    private Long receiptItemId;

    @NotNull
    @PositiveOrZero
    @JsonProperty("counted_qty")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer countedQty;

    public Long getReceiptItemId() {
        return receiptItemId;
    }

    public void setReceiptItemId(Long receiptItemId) {
        this.receiptItemId = receiptItemId;
    }

    public Integer getCountedQty() {
        return countedQty;
    }

    public void setCountedQty(Integer countedQty) {
        this.countedQty = countedQty;
    }
}
