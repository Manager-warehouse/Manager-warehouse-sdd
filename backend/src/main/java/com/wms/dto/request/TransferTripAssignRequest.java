package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TransferTripAssignRequest(
        @NotNull Long vehicleId,
        @NotNull Long driverId,
        @NotNull LocalDate plannedDate) {
}
