package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

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

    @JsonProperty("unit_cost")
    private BigDecimal unitCost;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_sku")
    private String productSku;

    @JsonProperty("qc_passed_qty")
    private Integer qcPassedQty;

    @JsonProperty("qc_failed_qty")
    private Integer qcFailedQty;



    @JsonProperty("location_id")
    private Long locationId;

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

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public Integer getQcPassedQty() {
        return qcPassedQty;
    }

    public void setQcPassedQty(Integer qcPassedQty) {
        this.qcPassedQty = qcPassedQty;
    }

    public Integer getQcFailedQty() {
        return qcFailedQty;
    }

    public void setQcFailedQty(Integer qcFailedQty) {
        this.qcFailedQty = qcFailedQty;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}

