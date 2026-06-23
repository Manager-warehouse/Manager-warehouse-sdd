package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripUpdateRequest {

    private Long vehicleId;

    private Long driverId;

    private LocalDateTime plannedStartAt;

    private LocalDateTime plannedEndAt;

    @Size(max = 1000)
    private String notes;

    @Valid
    @Size(min = 1)
    private List<TripDeliveryOrderRequest> deliveryOrders;
}
