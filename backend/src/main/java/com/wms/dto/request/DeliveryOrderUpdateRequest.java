package com.wms.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderUpdateRequest {

    private LocalDate expectedDeliveryDate;
    private String notes;
    private String cancelReason;
}
