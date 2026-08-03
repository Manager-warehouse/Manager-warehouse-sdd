package com.wms.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record InterWarehouseTransferUpdateRequest(
        @NotBlank(message = "EXTERNAL_INSTRUCTION_CODE_REQUIRED") String externalInstructionCode,
        @NotNull(message = "SOURCE_WAREHOUSE_ID_REQUIRED") Long sourceWarehouseId,
        @NotNull(message = "DESTINATION_WAREHOUSE_ID_REQUIRED") Long destinationWarehouseId,
        @NotNull(message = "DOCUMENT_DATE_REQUIRED") LocalDate documentDate,
        @NotNull(message = "PLANNED_DATE_REQUIRED") LocalDate plannedDate,
        String notes,
        @NotEmpty(message = "ITEMS_REQUIRED") List<@Valid InterWarehouseTransferItemRequest> items) {}
