package com.wms.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReturnRequest {
    @NotNull(message = "WAREHOUSE_REQUIRED")
    private Long warehouseId;

    @NotNull(message = "DEALER_REQUIRED")
    private Long dealerId;

    @NotNull(message = "DO_REQUIRED")
    private Long deliveryOrderId;

    private String notes;

    @NotEmpty(message = "ITEMS_REQUIRED")
    @Valid
    private List<CreateReturnItemRequest> items;
}
