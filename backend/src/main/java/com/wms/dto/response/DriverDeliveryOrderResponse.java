package com.wms.dto.response;


import com.wms.entity.order_fulfillment.Delivery;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DriverDeliveryOrderResponse {
    private Long doId;
    private String doNumber;
    private String dealerName;
    private String dealerAddress;
    private DeliveryOrderStatus status;
    private Integer stopOrder;
    private DeliveryAttemptResponse currentAttempt;

    // Split Delivery Milestone Data (US6)
    private Long splitPlanId;
    private Long splitLegId;
    private SplitDeliveryPlanStatus splitPlanStatus;
    private Boolean isSplitLead;
    private SplitDeliveryPlanStatus splitLegStatus;
    private OffsetDateTime readinessConfirmedAt;
    private OffsetDateTime dealerArrivedAt;
    private OffsetDateTime handoverConfirmedAt;
}
