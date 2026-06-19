package com.wms.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeliveryOrderAllocationResponse {
    private Long allocationId;
    private Long inventoryId;
    private Long batchId;
    private Long locationId;
    private Long zoneId;
    private BigDecimal plannedQty;
    private BigDecimal pickedQty;
    private boolean replacement;
}
