package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TransferReturnRequest(
    @NotBlank(message = "RETURN_REASON_REQUIRED")
    String reason
) {}
