package com.wms.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.billing_payment.CorrectionVoucherReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorrectionVoucherCreateRequest {

    @NotNull(message = "Reference type is required")
    @JsonProperty("reference_type")
    private CorrectionVoucherReferenceType referenceType;

    @NotNull(message = "Reference id is required")
    @JsonProperty("reference_id")
    private Long referenceId;

    @NotNull(message = "Amount delta is required")
    @JsonProperty("amount_delta")
    private BigDecimal amountDelta;

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotNull(message = "Document date is required")
    @JsonProperty("document_date")
    private LocalDate documentDate;
}
