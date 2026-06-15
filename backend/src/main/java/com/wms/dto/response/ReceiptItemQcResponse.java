package com.wms.dto.response;

import com.wms.enums.QcResult;
import com.wms.enums.QcSamplingMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReceiptItemQcResponse {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private BigDecimal sampleQty;
    private BigDecimal samplePassedQty;
    private BigDecimal sampleFailedQty;
    private QcSamplingMethod qcSamplingMethod;
    private QcResult qcResult;
    private String qcFailureReason;
    private Long qcByUserId;
}
