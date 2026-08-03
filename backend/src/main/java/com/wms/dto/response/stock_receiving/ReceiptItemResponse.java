package com.wms.dto.response.stock_receiving;


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

    @JsonProperty("qc_result")
    private String qcResult;

    @JsonProperty("qc_failure_reason")
    private String qcFailureReason;

    @JsonProperty("approved_qty")
    private Integer approvedQty;

    @JsonProperty("quarantine_ready_qty")
    private Integer quarantineReadyQty;

    @JsonProperty("quarantine_qty")
    private Integer quarantineQty;



    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("batch_id")
    private Long batchId;

    @JsonProperty("batch_code")
    private String batchCode;

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

    public String getQcResult() {
        return qcResult;
    }

    public void setQcResult(String qcResult) {
        this.qcResult = qcResult;
    }

    public String getQcFailureReason() {
        return qcFailureReason;
    }

    public void setQcFailureReason(String qcFailureReason) {
        this.qcFailureReason = qcFailureReason;
    }

    public Integer getApprovedQty() {
        return approvedQty;
    }

    public void setApprovedQty(Integer approvedQty) {
        this.approvedQty = approvedQty;
    }

    public Integer getQuarantineReadyQty() {
        return quarantineReadyQty;
    }

    public void setQuarantineReadyQty(Integer quarantineReadyQty) {
        this.quarantineReadyQty = quarantineReadyQty;
    }

    public Integer getQuarantineQty() {
        return quarantineQty;
    }

    public void setQuarantineQty(Integer quarantineQty) {
        this.quarantineQty = quarantineQty;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }
}

