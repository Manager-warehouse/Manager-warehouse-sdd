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
public class DisposalCreateResponse {
    private Long adjustmentId;
    private String adjustmentNumber;
    private Long damageReportId;
    private String damageReportNumber;
    private BigDecimal totalValueEstimate;
    private boolean confirmed;
    private String message;
}
