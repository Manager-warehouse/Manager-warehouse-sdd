package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderWarehouseRejectRequest {

    @NotBlank
    @Size(max = 1000)
    private String reason;

    @Valid
    private List<DeliveryOrderWarehouseRejectReturnRequest> returnToBinRecords;
}
