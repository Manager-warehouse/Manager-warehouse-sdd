package com.wms.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class CreditNoteResponse {
    private Long creditNoteId;
    private String creditNoteNumber;
    private Long dealerId;
    private String dealerName;
    private BigDecimal amount;
    private BigDecimal currentBalance;
    private String reason;
    private LocalDate documentDate;
    private OffsetDateTime createdAt;
}
