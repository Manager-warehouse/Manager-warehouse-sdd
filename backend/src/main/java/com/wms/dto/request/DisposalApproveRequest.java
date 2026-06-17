package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisposalApproveRequest {

    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;
}
