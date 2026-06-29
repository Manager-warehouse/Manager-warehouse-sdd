package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineItemResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("product_sku")
    private String productSku;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("qc_failed_qty")
    private Integer qcFailedQty;

    @JsonProperty("qc_failure_reason")
    private String qcFailureReason;

    @JsonProperty("receipt_number")
    private String receiptNumber;

    @JsonProperty("supplier_id")
    private Long supplierId;

    @JsonProperty("total_value")
    private BigDecimal totalValue;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("receipt_id")
    private Long receiptId;

    @JsonProperty("receipt_version")
    private Integer receiptVersion;

    @JsonProperty("origin_type")
    @Builder.Default
    private String originType = "RECEIPT";

    @JsonProperty("quarantine_record_id")
    private Long quarantineRecordId;
}
