package com.wms.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeliveryOrderItemResponse {
    private Long id;
    private Long productId;
    private Long batchId;
    private Long locationId;
    private Long zoneId;
    private String productName;
    private String sku;
    private BigDecimal requestedQty;
    private BigDecimal reservedQty;
    private BigDecimal plannedQty;
    private BigDecimal pickedQty;
    private BigDecimal qcPassQty;
    private BigDecimal qcFailQty;
    private BigDecimal issuedQty;
    private BigDecimal unitPrice;
    private List<DeliveryOrderAllocationResponse> allocations;
}
