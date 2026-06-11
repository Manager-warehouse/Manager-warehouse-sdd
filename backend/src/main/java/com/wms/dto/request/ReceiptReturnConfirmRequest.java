package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for Storekeeper confirming physical return of rejected goods to supplier.
 * Used by PUT /api/v1/receipts/{id}/return-to-supplier/confirm
 */
@Getter
@Setter
public class ReceiptReturnConfirmRequest {

    /** Optimistic locking: the version the client last read. Reject if changed (HTTP 409). */
    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    /** Optional handover reference (e.g. driver name, vehicle plate, note). */
    private String handoverNote;
}
