package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for Storekeeper confirming physical return of QC_FAILED goods to supplier.
 * Used by PUT /api/v1/receipts/{id}/rtv/confirm
 *
 * The returned quantity MUST equal the full quarantined quantity; partial returns are rejected (HTTP 422).
 */
@Getter
@Setter
public class ReceiptRtvConfirmRequest {

    /** Optimistic locking: the version of the RTV adjustment the client last read. */
    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    /**
     * Quantity physically returned to supplier.
     * Must equal the full quarantined quantity for the receipt; partial confirmation is rejected.
     */
    @NotNull(message = "RETURNED_QTY_REQUIRED")
    @DecimalMin(value = "0.01", message = "RETURNED_QTY_MUST_BE_POSITIVE")
    private BigDecimal returnedQty;
}
