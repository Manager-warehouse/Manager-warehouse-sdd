package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request DTO for Trưởng kho creating an RTV (Return To Vendor) request for a QC_FAILED receipt.
 * Used by POST /api/v1/receipts/{id}/rtv
 */
@Getter
@Setter
public class ReceiptRtvCreateRequest {

    /** Optimistic locking: the version the client last read. Reject if changed (HTTP 409). */
    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    /** Reason for returning the goods to vendor; required for audit and Debit Note. */
    @NotBlank(message = "REASON_REQUIRED")
    @Size(max = 2000, message = "REASON_TOO_LONG")
    private String reason;

    /** Document date for the RTV adjustment and Debit Note. Defaults to today if not provided. */
    private LocalDate documentDate;
}
