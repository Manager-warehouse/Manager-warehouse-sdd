package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wms.enums.QcSamplingMethod;
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
    private Long receiptItemId;

    @JsonProperty("sample_qty")
    private Integer sampleQty;

    @NotNull
    @Min(0)
    @JsonProperty("qc_passed_qty")
    private Integer qcPassedQty;

    @NotNull
    @Min(0)
    @JsonProperty("qc_failed_qty")
    private Integer qcFailedQty;

    @JsonProperty("qc_sampling_method")
    private QcSamplingMethod qcSamplingMethod;

    @JsonProperty("qc_failure_reason")
    private String qcFailureReason;

}
