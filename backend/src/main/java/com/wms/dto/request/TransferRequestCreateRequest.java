package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TransferRequestCreateRequest(
    @NotNull(message = "SOURCE_WAREHOUSE_ID_REQUIRED")
    Long sourceWarehouseId,

    @NotNull(message = "DESTINATION_WAREHOUSE_ID_REQUIRED")
    Long destinationWarehouseId,

    LocalDate neededByDate,

    String businessReason,

    String notes,

    @NotEmpty(message = "ITEMS_REQUIRED")
    @Valid
    List<TransferRequestItemRequest> items
) {}
