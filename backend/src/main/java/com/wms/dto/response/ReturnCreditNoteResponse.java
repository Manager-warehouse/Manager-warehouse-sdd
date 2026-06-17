package com.wms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCreditNoteResponse {
    private Long creditNoteId;
    private String creditNoteNumber;
    private BigDecimal amount;
    private Long dealerId;
    private String message;
}
