package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceiptItemResponse {

    @JsonProperty("receipt_item_id")
    private Long receiptItemId;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("expected_qty")
    private Integer expectedQty;

    @JsonProperty("actual_qty")
    private Integer actualQty;

    @JsonProperty("over_received_qty")
    private Integer overReceivedQty;

    public Long getReceiptItemId() {
        return receiptItemId;
    }

    public void setReceiptItemId(Long receiptItemId) {
        this.receiptItemId = receiptItemId;
    }

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

    public Integer getActualQty() {
        return actualQty;
    }

    public void setActualQty(Integer actualQty) {
        this.actualQty = actualQty;
    }

    public Integer getOverReceivedQty() {
        return overReceivedQty;
    }

    public void setOverReceivedQty(Integer overReceivedQty) {
        this.overReceivedQty = overReceivedQty;
    }
}
