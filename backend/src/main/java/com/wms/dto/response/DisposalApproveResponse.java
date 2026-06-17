package com.wms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisposalApproveResponse {
    private Long adjustmentId;
    private String adjustmentNumber;
    private boolean confirmed;
    private Long approvedBy;
    private OffsetDateTime approvedAt;
    private BigDecimal deductedQty;
    private String message;
}
