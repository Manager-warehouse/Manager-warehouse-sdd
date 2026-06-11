package com.wms.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for RTV create/confirm actions.
 * Returns the RTV adjustment and Debit Note identifiers along with quarantine quantity.
 */
@Getter
@Builder
public class RtvActionResponse {

    private Long adjustmentId;
    private String adjustmentNumber;
    private Long debitNoteId;
    private String debitNoteNumber;
    private BigDecimal quarantineQty;
    private boolean confirmed;
    private OffsetDateTime confirmedAt;
    private String message;
}
