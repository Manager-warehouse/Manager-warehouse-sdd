package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderPickQcResultRequest {

    @Size(max = 100)
    private String idempotencyKey;

    @Valid
    @NotEmpty
    private List<DeliveryOrderPickQcRowRequest> results;
}
