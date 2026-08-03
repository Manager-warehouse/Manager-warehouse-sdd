package com.wms.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.billing_payment.CorrectionVoucherReferenceType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    // Matches adjustments.amount_delta NUMERIC(18,2) - reject before it becomes an
    // opaque DataIntegrityViolationException at the DB layer.
    @Digits(integer = 16, fraction = 2, message = "AMOUNT_TOO_LARGE")
    @JsonProperty("amount_delta")
    private BigDecimal amountDelta;

    @NotBlank(message = "Reason is required")
    @Size(max = 2000, message = "REASON_TOO_LONG")
    private String reason;

    @NotNull(message = "Document date is required")
    @JsonProperty("document_date")
    private LocalDate documentDate;
}
