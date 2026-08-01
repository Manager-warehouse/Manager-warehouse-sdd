package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wms.config.jackson.StrictIntegerDeserializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ReceiveQcReceiptItemRequest {

    @NotNull
    @JsonProperty("receipt_item_id")
    @JsonAlias("receiptItemId")
    private Long receiptItemId;

    @NotNull
    @PositiveOrZero
    @JsonProperty("actual_qty")
    @JsonAlias("actualQty")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer actualQty;

    @NotNull
    @PositiveOrZero
    @JsonProperty("quality_passed_qty")
    @JsonAlias({"qualityPassedQty", "qc_passed_qty", "qcPassedQty"})
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer qualityPassedQty;

    @NotNull
    @PositiveOrZero
    @JsonProperty("quality_failed_qty")
    @JsonAlias({"qualityFailedQty", "qc_failed_qty", "qcFailedQty"})
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer qualityFailedQty;

    @JsonProperty("qc_failure_reason")
    @JsonAlias({"qcFailureReason", "failure_reason", "failureReason"})
    private String qcFailureReason;

    public Long getReceiptItemId() {
        return receiptItemId;
    }

    public void setReceiptItemId(Long receiptItemId) {
        this.receiptItemId = receiptItemId;
    }

    public Integer getActualQty() {
        return actualQty;
    }

    public void setActualQty(Integer actualQty) {
        this.actualQty = actualQty;
    }

    public Integer getQualityPassedQty() {
        return qualityPassedQty;
    }

    public void setQualityPassedQty(Integer qualityPassedQty) {
        this.qualityPassedQty = qualityPassedQty;
    }

    public Integer getQualityFailedQty() {
        return qualityFailedQty;
    }

    public void setQualityFailedQty(Integer qualityFailedQty) {
        this.qualityFailedQty = qualityFailedQty;
    }

    public String getQcFailureReason() {
        return qcFailureReason;
    }

    public void setQcFailureReason(String qcFailureReason) {
        this.qcFailureReason = qcFailureReason;
    }
}
