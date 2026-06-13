package com.wms.dto.response;

import com.wms.enums.QcResult;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TransferItemResponse {
    private Long id;
    private Long productId;
    private Long batchId;
    private Long sourceLocationId;
    private Long destinationLocationId;
    private BigDecimal plannedQty;
    private BigDecimal sentQty;
    private BigDecimal receivedQty;
    private BigDecimal varianceQty;
    private BigDecimal qcPassedQty;
    private BigDecimal qcFailedQty;
    private QcResult qcResult;
    private String qcFailureReason;
}
