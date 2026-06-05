package com.wms.dto.request;

import com.wms.enums.DeliveryOrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderCreateRequest {

    @NotNull
    private Long dealerId;

    @NotNull
    private Long warehouseId;

    @NotNull
    private DeliveryOrderType type;

    private LocalDate expectedDeliveryDate;

    @NotNull
    private LocalDate documentDate;

    private String notes;

    @Valid
    @NotEmpty
    private List<DeliveryOrderItemCreateRequest> items;
}
