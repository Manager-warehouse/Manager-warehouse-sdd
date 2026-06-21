package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Request DTO for Storekeeper completing putaway after Trưởng kho approval.
 * Used by PUT /api/v1/receipts/{id}/complete
 *
 * Putaway ONLY increases regular inventory; locations must be non-quarantine Bins.
 */
@Getter
@Setter
public class ReceiptPutawayRequest {

    /** Optimistic locking: the version the client last read. Reject if changed (HTTP 409). */
    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    /** Target regular Bin locations per line item. */
    @NotEmpty(message = "ITEMS_REQUIRED")
    @Valid
    private List<ReceiptPutawayItem> items;
}
