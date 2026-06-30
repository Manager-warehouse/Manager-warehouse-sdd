package com.wms.dto.request;

import jakarta.validation.constraints.Size;

public record InterWarehouseTransferFinalReceiveRequest(
        @Size(max = 1000)
        String discrepancyReason) {}
