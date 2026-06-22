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
    private String driverName;
    private Long vehicleId;
    private String vehiclePlate;
    private String plannedDate;
    private List<DriverDeliveryOrderResponse> deliveryOrders;
}
