package com.wms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PickingCandidateResponse {
    private Long inventoryId;
    private Long batchId;
    private String batchCode;
    private Long locationId;
    private String locationCode;
    private Long zoneId;
    private String zoneCode;
    private BigDecimal availableQty;
    private LocalDate receivedDate;
}
