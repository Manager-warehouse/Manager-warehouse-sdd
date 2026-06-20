package com.wms.dto.response;

import com.wms.enums.DeliveryOrderStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TripDeliveryOrderResponse {
    private Long doId;
    private String doNumber;
    private Long warehouseId;
    private DeliveryOrderStatus status;
    private Integer stopOrder;
}
