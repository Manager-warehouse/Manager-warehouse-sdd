package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TransferUpdateRequest(
        @NotBlank String externalInstructionCode,
        @NotNull Long sourceWarehouseId,
        @NotNull Long destinationWarehouseId,
        @NotNull LocalDate documentDate,
        LocalDate plannedDate,
        String notes,
        @NotEmpty List<@Valid TransferItemRequest> items) {
}
