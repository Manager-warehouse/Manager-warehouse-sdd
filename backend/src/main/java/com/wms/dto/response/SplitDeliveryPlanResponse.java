package com.wms.dto.response;

import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SplitDeliveryPlanResponse {
    private Long id;
    private String planNumber;
    private Long doId;
    private Long warehouseId;
    private Long dispatcherId;
    private Long leadDriverId;
    private SplitDeliveryPlanStatus status;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private OffsetDateTime departedAt;
    private Integer readyDriverCount;
    private Integer totalDriverCount;
    private List<SplitDeliveryLegResponse> legs;
}
