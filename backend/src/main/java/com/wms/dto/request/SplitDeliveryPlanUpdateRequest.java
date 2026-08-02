package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitDeliveryPlanUpdateRequest {

    private Long leadDriverId;

    private LocalDateTime plannedStartAt;

    private LocalDateTime plannedEndAt;

    @Valid
    @Size(min = 2)
    private List<SplitDeliveryLegRequest> legs;
}
