package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for Storekeeper completing putaway after Trưởng kho approval.
 * Used by PUT /api/v1/receipts/{id}/complete
 *
 * Putaway ONLY increases regular inventory; location must be a non-quarantine Bin.
 */
@Getter
@Setter
public class ReceiptPutawayRequest {

    /** Optimistic locking: the version the client last read. Reject if changed (HTTP 409). */
    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    /** Target regular Bin location for putaway. Must have is_quarantine = false. */
    @NotNull(message = "LOCATION_ID_REQUIRED")
    private Long locationId;
}

