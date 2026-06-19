package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for approve/reject decision on a QC_COMPLETED receipt.
 * Used by PUT /api/v1/receipts/{id}/approve and PUT /api/v1/receipts/{id}/reject
 *
 * For reject: reason is mandatory (validated at service layer).
 * For approve: reason is optional.
 */
@Getter
@Setter
public class ReceiptDecisionRequest {

    /** Optimistic locking: the version the client last read. Reject if changed (HTTP 409). */
    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    /** Required for reject; optional for approve. Validated at service layer. */
    @Size(max = 2000, message = "REASON_TOO_LONG")
    private String reason;
}

