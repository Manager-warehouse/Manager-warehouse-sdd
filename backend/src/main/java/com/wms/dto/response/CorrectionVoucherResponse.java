package com.wms.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.billing_payment.CorrectionVoucherReferenceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionVoucherResponse {

    private Long id;

    @JsonProperty("adjustment_number")
    private String adjustmentNumber;

    @JsonProperty("reference_type")
    private CorrectionVoucherReferenceType referenceType;

    @JsonProperty("reference_id")
    private Long referenceId;

    @JsonProperty("dealer_id")
    private Long dealerId;

    @JsonProperty("dealer_name")
    private String dealerName;

    @JsonProperty("supplier_id")
    private Long supplierId;

    @JsonProperty("supplier_name")
    private String supplierName;

    @JsonProperty("amount_delta")
    private BigDecimal amountDelta;

    private String reason;

    @JsonProperty("document_date")
    private LocalDate documentDate;

    @JsonProperty("accounting_period_id")
    private Long accountingPeriodId;

    @JsonProperty("accounting_period_name")
    private String accountingPeriodName;

    // The CLOSED period the reference document originally belonged to - distinct from
    // accountingPeriodId above, which is the currently-OPEN period this voucher itself
    // is posted in. Lets /finance/periods show corrections nested under the closed
    // period they relate to.
    @JsonProperty("original_period_id")
    private Long originalPeriodId;

    @JsonProperty("original_period_name")
    private String originalPeriodName;

    @JsonProperty("approved_by")
    private Long approvedById;

    @JsonProperty("approved_by_name")
    private String approvedByName;

    @JsonProperty("approved_at")
    private OffsetDateTime approvedAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
