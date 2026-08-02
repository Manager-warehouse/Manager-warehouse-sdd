package com.wms.dto.response;

import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SplitLegMilestoneResponse {
    private Long splitPlanId;
    private Long legId;
    private SplitDeliveryPlanStatus status;
    private OffsetDateTime dealerArrivedAt;
    private OffsetDateTime handoverConfirmedAt;
    private OffsetDateTime failureReportedAt;
    private boolean allLegsArrived;
    private boolean allLegsHandedOver;
    private boolean leadPodOtpEnabled;
}
