package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InterWarehouseTransferReasonRequest(
        @NotBlank
        @Size(max = 1000)
        String reason) {}
