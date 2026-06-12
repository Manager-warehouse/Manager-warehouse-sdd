package com.wms.dto.request;

import com.wms.enums.QcSamplingMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReceiptQcItemRequest {

    @NotNull
    private Long receiptItemId;

    @NotNull
    @DecimalMin("0")
    private BigDecimal sampleQty;

    @NotNull
    @DecimalMin("0")
    private BigDecimal samplePassedQty;

    @NotNull
    @DecimalMin("0")
    private BigDecimal sampleFailedQty;

    /** Nếu null, hệ thống tự xác định theo số lần APPROVED của nhà cung cấp. */
    private QcSamplingMethod qcSamplingMethod;

    private String qcFailureReason;
}
