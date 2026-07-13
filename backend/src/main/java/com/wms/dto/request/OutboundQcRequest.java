package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OutboundQcRequest(
    @NotNull(message = "QC_RESULT_REQUIRED")
    Boolean passed,

    String note,

    @NotBlank(message = "PHOTO_REF_REQUIRED")
    String photoRef
) {}
