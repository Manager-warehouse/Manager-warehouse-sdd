package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TransferReturnRejectRequest(
    @NotBlank(message = "REJECTION_REASON_REQUIRED")
    String reason
) {}
