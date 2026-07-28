package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DiscrepancyIncidentResolveRequest(
        @NotBlank(message = "DISCREPANCY_RESOLUTION_STATUS_REQUIRED")
        @Pattern(
                regexp = "RESOLVED_ACCEPTED|RESOLVED_SOURCE_FAULT|RESOLVED_CARRIER_FAULT|RESOLVED_DESTINATION_COUNT_ERROR",
                message = "DISCREPANCY_RESOLUTION_STATUS_INVALID"
        )
        String status,

        @NotBlank(message = "DISCREPANCY_RESOLUTION_NOTE_REQUIRED")
        @Size(max = 1000, message = "DISCREPANCY_RESOLUTION_NOTE_TOO_LONG")
        String resolutionNote
) {
}
