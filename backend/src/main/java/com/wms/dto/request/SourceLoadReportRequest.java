package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SourceLoadReportRequest(
        @NotEmpty(message = "SOURCE_LOAD_ITEMS_REQUIRED")
        List<@Valid SourceLoadReportItemRequest> items,

        String reworkReason
) {}
