package com.wms.dto.response;

import com.wms.entity.ReceiptItem;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceiptItemResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Long batchId;
    private Long locationId;
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private BigDecimal sampleQty;
    private BigDecimal samplePassedQty;
    private BigDecimal sampleFailedQty;
    private String qcSamplingMethod;
    private String qcResult;
    private String qcFailureReason;
    private BigDecimal unitCost;

    public static ReceiptItemResponse from(ReceiptItem item) {
        return ReceiptItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productSku(item.getProduct().getSku())
                .productName(item.getProduct().getName())
                .batchId(item.getBatch() != null ? item.getBatch().getId() : null)
                .locationId(item.getLocation() != null ? item.getLocation().getId() : null)
                .expectedQty(item.getExpectedQty())
                .actualQty(item.getActualQty())
                .sampleQty(item.getSampleQty())
                .samplePassedQty(item.getSamplePassedQty())
                .sampleFailedQty(item.getSampleFailedQty())
                .qcSamplingMethod(item.getQcSamplingMethod() != null ? item.getQcSamplingMethod().name() : null)
                .qcResult(item.getQcResult() != null ? item.getQcResult().name() : null)
                .qcFailureReason(item.getQcFailureReason())
                .unitCost(item.getUnitCost())
                .build();
    }
}
