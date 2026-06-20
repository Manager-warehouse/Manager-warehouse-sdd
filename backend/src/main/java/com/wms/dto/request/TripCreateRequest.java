package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripCreateRequest {

    @NotNull
    private Long warehouseId;

    @NotNull
    private Long vehicleId;

    @NotNull
    private Long driverId;

    @NotNull
    private LocalDate plannedDate;

    @Size(max = 1000)
    private String notes;

    @Valid
    @NotEmpty
    private List<TripDeliveryOrderRequest> deliveryOrders;
}
