package com.wms.dto.response;

import com.wms.enums.DeliveryOrderStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DriverDeliveryOrderResponse {
    private Long doId;
    private String doNumber;
    private DeliveryOrderStatus status;
    private Integer stopOrder;
    private DeliveryAttemptResponse currentAttempt;
}
