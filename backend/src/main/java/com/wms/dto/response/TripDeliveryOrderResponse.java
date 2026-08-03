package com.wms.dto.response;


import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TripDeliveryOrderResponse {
    private Long doId;
    private String doNumber;
    private String dealerName;
    private String dealerAddress;
    private Long warehouseId;
    private DeliveryOrderStatus status;
    private Integer stopOrder;
    private Long splitPlanId;
    private Long leadDriverId;
    private SplitDeliveryPlanStatus splitPlanStatus;
    private Boolean isSplitLead;
}
