package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitDeliveryPlanCreateRequest {

    @NotNull
    private Long doId;

    @NotNull
    private Long leadDriverId;

    @NotNull
    private LocalDateTime plannedStartAt;

    @NotNull
    private LocalDateTime plannedEndAt;

    @Valid
    @NotEmpty
    @Size(min = 2)
    private List<SplitDeliveryLegRequest> legs;
}
