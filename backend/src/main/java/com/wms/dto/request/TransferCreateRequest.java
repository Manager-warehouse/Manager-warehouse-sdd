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
public class TransferCreateRequest {

    @NotNull
    private Long sourceWarehouseId;

    @NotNull
    private Long destinationWarehouseId;

    private LocalDate plannedDate;

    @NotNull
    private LocalDate documentDate;

    @Size(max = 80)
    private String externalInstructionCode;

    private String notes;

    @Valid
    @NotEmpty
    private List<TransferItemRequest> items;
}
