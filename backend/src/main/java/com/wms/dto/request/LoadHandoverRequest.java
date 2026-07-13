package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoadHandoverRequest(
    @NotBlank(message = "PHOTO_REF_REQUIRED")
    String photoRef
) {}
