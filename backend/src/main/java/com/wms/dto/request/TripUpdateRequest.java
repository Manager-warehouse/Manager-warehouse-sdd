package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripUpdateRequest {

    private Long vehicleId;

    private Long driverId;

    private LocalDate plannedDate;

    private OffsetDateTime plannedStartAt;

    private OffsetDateTime plannedEndAt;

    @Size(max = 1000)
    private String notes;

    @Valid
    @Size(min = 1)
    private List<TripDeliveryOrderRequest> deliveryOrders;
}
