package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.QcResult;
import com.wms.enums.QcSamplingMethod;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceiptItemQcResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("expected_qty")
    private Integer expectedQty;

    @JsonProperty("actual_qty")
    private Integer actualQty;

    @JsonProperty("sample_qty")
    private Integer sampleQty;

    @JsonProperty("sample_passed_qty")
    private Integer samplePassedQty;

    @JsonProperty("sample_failed_qty")
    private Integer sampleFailedQty;

    @JsonProperty("qc_sampling_method")
    private QcSamplingMethod qcSamplingMethod;

    @JsonProperty("qc_result")
    private QcResult qcResult;

    @JsonProperty("qc_failure_reason")
    private String qcFailureReason;

    @JsonProperty("qc_by_user_id")
    private Long qcByUserId;
}
