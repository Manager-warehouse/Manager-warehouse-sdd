package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record InterWarehouseTransferCreateRequest(
        @NotEmpty String externalInstructionCode,
        @NotNull Long sourceWarehouseId,
        @NotNull Long destinationWarehouseId,
        @NotNull LocalDate documentDate,
        @NotNull LocalDate plannedDate,
        String notes,
        @NotEmpty List<@Valid InterWarehouseTransferItemRequest> items) {}
