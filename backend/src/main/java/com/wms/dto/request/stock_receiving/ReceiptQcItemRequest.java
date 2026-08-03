package com.wms.dto.request.stock_receiving;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.stock_receiving.QcSamplingMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiptQcItemRequest {

    @NotNull
    @JsonProperty("receipt_item_id")
    @JsonAlias("receiptItemId")
    private Long receiptItemId;

    @JsonProperty("sample_qty")
    @JsonAlias("sampleQty")
    private Integer sampleQty;

    @NotNull
    @Min(0)
    @JsonProperty("qc_passed_qty")
    @JsonAlias("samplePassedQty")
    private Integer qcPassedQty;

    @NotNull
    @Min(0)
    @JsonProperty("qc_failed_qty")
    @JsonAlias("sampleFailedQty")
    private Integer qcFailedQty;

    @Min(0)
    @JsonProperty("quality_failed_qty")
    @JsonAlias("qualityFailedQty")
    private Integer qualityFailedQty;

    @Min(0)
    @JsonProperty("quality_passed_qty")
    @JsonAlias("qualityPassedQty")
    private Integer qualityPassedQty;

    @JsonProperty("qc_sampling_method")
    private QcSamplingMethod qcSamplingMethod;

    @JsonProperty("qc_failure_reason")
    private String qcFailureReason;

}
