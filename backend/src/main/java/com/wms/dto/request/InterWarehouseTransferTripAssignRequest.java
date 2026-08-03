package com.wms.dto.request;


import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InterWarehouseTransferTripAssignRequest(
        @NotNull(message = "VEHICLE_ID_REQUIRED") Long vehicleId,
        @NotNull(message = "DRIVER_ID_REQUIRED") Long driverId,
        @NotNull(message = "PLANNED_START_AT_REQUIRED") LocalDateTime plannedStartAt,
        @NotNull(message = "PLANNED_END_AT_REQUIRED") LocalDateTime plannedEndAt) {}
