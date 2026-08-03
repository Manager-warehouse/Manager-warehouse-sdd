package com.wms.dto.response;

import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SplitDeliveryLegResponse {
    private Long id;
    private Long tripId;
    private Long vehicleId;
    private Long driverId;
    private Integer stopOrder;
    private SplitDeliveryPlanStatus status;
    private OffsetDateTime readinessConfirmedAt;
    private OffsetDateTime departedAt;
    private OffsetDateTime dealerArrivedAt;
    private OffsetDateTime handoverConfirmedAt;
    private OffsetDateTime failureReportedAt;
    private String failureReason;
}
