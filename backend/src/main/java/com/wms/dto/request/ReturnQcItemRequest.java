package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReturnQcItemRequest {

    @NotNull(message = "RECEIPT_ITEM_ID_REQUIRED")
    private Long receiptItemId;

    @NotNull(message = "ACTUAL_QUANTITY_REQUIRED")
    @DecimalMin(value = "0.00", message = "ACTUAL_QUANTITY_CANNOT_BE_NEGATIVE")
    private BigDecimal actualQty;

    @NotNull(message = "QC_PASSED_QUANTITY_REQUIRED")
    @DecimalMin(value = "0.00", message = "QC_PASSED_QUANTITY_CANNOT_BE_NEGATIVE")
    private BigDecimal qcPassedQty;

    @NotNull(message = "QC_FAILED_QUANTITY_REQUIRED")
    @DecimalMin(value = "0.00", message = "QC_FAILED_QUANTITY_CANNOT_BE_NEGATIVE")
    private BigDecimal qcFailedQty;

    @Size(max = 2000, message = "QC_FAILURE_REASON_TOO_LONG")
    private String qcFailureReason;
}
