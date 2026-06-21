package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InterWarehouseTransferTripAssignRequest(
        @NotNull Long vehicleId,
        @NotNull Long driverId,
        @NotNull LocalDateTime plannedStartAt,
        @NotNull LocalDateTime plannedEndAt) {}
