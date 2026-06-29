package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TransferRequestRejectRequest(
    @NotBlank(message = "REJECTION_REASON_REQUIRED")
    String reason
) {}
