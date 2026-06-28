package com.wms.dto.response;

import com.wms.enums.TripStatus;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TripDriverViewResponse {
    private Long tripId;
    private String tripNumber;
    private TripStatus status;
    private Long driverId;
    private Long vehicleId;
    private List<DriverDeliveryOrderResponse> deliveryOrders;
}
